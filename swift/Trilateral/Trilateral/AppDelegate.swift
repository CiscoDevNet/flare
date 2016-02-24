//
//  AppDelegate.swift
//  Trilateral
//
//  Created by Andrew Zamler-Carhart on 3/24/15.
//  Copyright (c) 2015 Cisco. All rights reserved.
//

import UIKit
import Flare
import CoreLocation
import WatchConnectivity

// Each of the UIView(Controller)s that control the various tabs conforms to this protocol.
// When the current objects change these variables will be set, and when the objects' data
// changes the dataChanged() function will be called.
protocol FlareController {
    var currentEnvironment: Environment? { get set }
    var currentZone: Zone? { get set }
    var device: Device? { get set }
    var nearbyThing: Thing? { get set }
    
    func dataChanged()
    func animate()
}

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate, CLLocationManagerDelegate, FlareManagerDelegate, BeaconManagerDelegate, WCSessionDelegate {

    var window: UIWindow?

    var host = "localhost"
    var port = 1234
    
    let animationDelay = 0.5 // the duration of the animation in seconds
    let animationSteps = 30 // the number of times during the animation that the display is updated
    
    var defaults = NSUserDefaults.standardUserDefaults()
    var flareManager = FlareManager(host: "localhost", port: 1234)
    var beaconManager = BeaconManager()
    var currentLocation: CLLocation?
    
    var allEnvironments = [Environment]()
    
    // when these are changed, the equivalent variables in the current flareController will be updated
    var currentEnvironment: Environment? { didSet(value) {
        if flareController != nil { flareController!.currentEnvironment = self.currentEnvironment }}}
    var currentZone: Zone? { didSet(value) {
        if flareController != nil { flareController!.currentZone = self.currentZone }}}
    var device: Device? { didSet(value) {
        if flareController != nil { flareController!.device = self.device }}}
    var nearbyThing: Thing? { didSet(value) {
        if flareController != nil { flareController!.nearbyThing = self.nearbyThing }}}
    
    // when the tab is changed, the new flareController should call updateFlareController()
    var flareController: FlareController? = nil
    
    var session: WCSession?
    
    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
        registerDefaultsFromSettingsBundle()

        if let newHost = defaults.stringForKey("host") { host = newHost }
        let newPort = defaults.integerForKey("port")
        if newPort != 0 { port = newPort }
        
        NSLog("Server: \(host):\(port)")
        NSLog("GPS: \(defaults.boolForKey("useGPS") ? "on" : "off")")
        NSLog("Beacons: \(defaults.boolForKey("useBeacons") ? "on" : "off")")
        NSLog("CMX: \(defaults.boolForKey("useCMX") ? "on" : "off")")
        NSLog("Compass: \(defaults.boolForKey("useCompass") ? "on" : "off")")
        
        flareManager = FlareManager(host: host, port: port)
        flareManager.debugSocket = false // turn on to print all Socket.IO messages
        
        flareManager.delegate = self
        beaconManager.delegate = self
        
        flareManager.connect()
        
        if !defaults.boolForKey("useGPS") { loadDefaultEnvironment() }

        if WCSession.isSupported() {
            session = WCSession.defaultSession()
            session!.delegate = self
            session!.activateSession()
        }
        
        return true
    }

    // called at startup, and when the GPS location changes significantly
    func deviceLocationDidChange(location: CLLocation) {
        NSLog("Location: \(location.coordinate.latitude),\(location.coordinate.longitude)")
        currentLocation = location
        
        loadEnvironments()
    }
    
    func loadEnvironments() {
        var params: JSONDictionary? = nil
        if currentLocation != nil {
            params = ["latitude":currentLocation!.coordinate.latitude, "longitude":currentLocation!.coordinate.longitude]
        }
        
        self.flareManager.loadEnvironments(params, loadDevices: false) { (environments) -> () in // load environment for current location
            if environments.count > 0 {
                self.allEnvironments = environments
                self.loadEnvironment(environments[0])
            } else {
                self.flareManager.loadEnvironments(nil, loadDevices: false) { (environments) -> () in // load all environments
                    if environments.count > 0 {
                        NSLog("No environments found nearby, using first one.")
                        self.allEnvironments = environments
                        self.loadEnvironment(environments[0])
                    } else {
                        NSLog("No environments found.")
                    }
                }
            }
        }
    }
    
    func loadDefaultEnvironment() {
        self.flareManager.loadEnvironments(nil, loadDevices: false) { (environments) -> () in // load all environments
            if environments.count > 0 {
                NSLog("Using default environment.")
                self.loadEnvironment(environments[0])
            } else {
                NSLog("No environments found.")
            }
        }
    }
    
    func loadEnvironment(environment: Environment) {
        self.currentEnvironment = environment
        self.flareManager.subscribe(environment, all: true)
        NSLog("Current environment: \(environment.name)")
        
        self.flareManager.getCurrentDevice(environment.id, template: self.deviceTemplate()) { (device) -> () in
            self.loadDevice(device)
        }
        
        self.beaconManager.loadEnvironment(environment)
        if defaults.boolForKey("useBeacons") { self.beaconManager.start() }
        if defaults.boolForKey("useGPS") { self.beaconManager.startMonitoringLocation() }
        if defaults.boolForKey("useCompass") { self.beaconManager.startUpdatingHeading() }
        
        self.updateFlareController()

    }
    
    func toggleEnvironment() {
        if allEnvironments.count > 0 && currentEnvironment != nil {
            let index = allEnvironments.indexOf(currentEnvironment!)
            let next = (index! + 1) % allEnvironments.count
            let nextEnvironment = allEnvironments[next]
            loadEnvironment(nextEnvironment)
        }
    }
    
    func loadDevice(value: Device?) {
        if let device = value {
            self.device = device
            // already subscribing to all objects
            // self.flareManager.subscribe(device)
            
            loadCurrentZone()
            loadNearbyThing()
            // loadMacAddress() // this is done server-side
        }
    }
    
    func loadCurrentZone() {
        if currentEnvironment != nil && device != nil {
            flareManager.getCurrentZone(currentEnvironment!, device: device!) { zone in
                self.currentZone = zone
            }
        }
    }
    
    func loadNearbyThing() {
        if currentEnvironment != nil && device != nil {
            flareManager.getNearestThing(currentEnvironment!, device: device!) { thing in
                self.nearbyThing = thing
            }
        }
    }
    
    func loadMacAddress() {
        if device != nil {
            if let mac = device!.data["mac"] as? String {
                if mac == "02:00:00:00:00:00" { // bogus
                    getMacAddress()
                }
            } else {
                getMacAddress()
            }
        }
    }
    
    func getMacAddress() {
        flareManager.getMacAddress(host, port: 80) { mac in
            NSLog("mac: \(mac)")
            self.flareManager.setData(self.device!, key: "mac", value: mac, sender: self.device!)
        }
    }
    
    func updateFlareController() {
        if flareController != nil {
            flareController!.currentEnvironment = self.currentEnvironment
            flareController!.currentZone = self.currentZone
            flareController!.device = self.device
            flareController!.nearbyThing = self.nearbyThing
        }
    }
    
    func dataChanged() {
        if flareController != nil {
            flareController!.dataChanged()
        }
    }
    
    func animate() {
        if flareController != nil {
            flareController!.animate()
        }
    }
    
    // returns a template used for creating new device objects:
    // name: Andrew's iPhone
    // description: iPhone, iOS 9.2
    // data: {}
    // postion: {"x":0, "y":0}
    func deviceTemplate() -> JSONDictionary {
        let uidevice = UIDevice.currentDevice()
        let name = uidevice.name
        let description = "\(uidevice.model), iOS \(uidevice.systemVersion)"
        return ["name":name, "description":description, "data":JSONDictionary(), "position":["x":0, "y":0]]
    }
    
    func applicationWillResignActive(application: UIApplication) {
        if defaults.boolForKey("useBeacons") { beaconManager.stop() }
        if defaults.boolForKey("useGPS") { beaconManager.stopMonitoringLocation() }
        if defaults.boolForKey("useCompass") { beaconManager.stopUpdatingHeading() }
        
        // not necessary to unsubscribe as disconnecting will take care of that
        flareManager.disconnect()
    }

    func applicationDidEnterBackground(application: UIApplication) {

    }

    func applicationWillEnterForeground(application: UIApplication) {
    
    }

    func applicationDidBecomeActive(application: UIApplication) {
        flareManager.connect()
        
        if currentLocation != nil {
            loadEnvironments() // reload the data and resubscribe
        }
        
        if defaults.boolForKey("useBeacons") { beaconManager.start() }
        if defaults.boolForKey("useGPS") { beaconManager.startMonitoringLocation() }
        if defaults.boolForKey("useCompass") { beaconManager.startUpdatingHeading() }
    }

    func applicationWillTerminate(application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }

    func devicePositionDidChange(position: CGPoint) {
        if device != nil {
            animateFlare(device!, oldPosition: device!.position, newPosition: position)
            flareManager.setPosition(device!, position: position, sender: nil)
            sendMessage(["position": position.toJSON()])
            
            if nearbyThing != nil {
                let distance = device!.distanceTo(nearbyThing!)
                let brightness = 1.0 - (distance)
                if brightness > 0 {
                    nearbyThing!.data["brightness"] = brightness
                    flareManager.setData(nearbyThing!, key: "brightness", value: brightness, sender: device!)
                    dataChanged()
                }
            }
        }
    }
    
    func deviceAngleDidChange(angle: Double) {
        if device != nil {
            if angle != device!.angle() {
                // NSLog("Angle: \(angle)")
                animateAngle(device!, oldAngle: device!.angle(), newAngle: angle)
                flareManager.setData(device!, key: "angle", value: angle, sender: device!)
                dataChanged()
            }
        }
    }
    
    func didReceiveData(flare: Flare, data: JSONDictionary, sender: Flare?) {
        dataChanged()
    }
    
    func didReceivePosition(flare: Flare, oldPosition: CGPoint, newPosition: CGPoint, sender: Flare?) {
        if defaults.boolForKey("useCMX") {
            NSLog("\(flare.name) position: \(newPosition)")
            animateFlare(flare as! FlarePosition, oldPosition: oldPosition, newPosition: newPosition)
        }
    }
    
    var didEnter = false // for half a second after an enter message arrives, ignore exit messages
    
    func enter(zone: Zone, device: Device) {
        NSLog("\(zone.name) enter: \(device.name)")
        self.currentZone = zone
        didEnter = true
        delay(0.5) { self.didEnter = false }
    }
    
    func exit(zone: Zone, device: Device) {
        NSLog("\(zone.name) exit: \(device.name)")
        if !didEnter {
            self.currentZone = nil
        } else {
            NSLog("Ignoring exit message!")
        }
    }
    
    func near(thing: Thing, device: Device, distance: Double) {
        NSLog("near: \(thing.name)")
        
        if device == self.device && thing != self.nearbyThing {
            nearbyThing = thing
            
            // already subscribing to all objects
            // flareManager.subscribe(thing)
            flareManager.getData(thing)
            flareManager.getPosition(thing)
            
        }
    }
    
    func far(thing: Thing, device: Device) {
        NSLog("far: \(thing.name)")

        if device == self.device && thing == self.nearbyThing {
            
            // already subscribing to all objects
            // flareManager.unsubscribe(thing)
            
            // stay paired even when moving away
            // nearbyThing = nil
        }
    }

    func animateFlare(var flare: FlarePosition, oldPosition: Point3D, newPosition: Point3D) {
        if oldPosition == newPosition { return }
        
        let dx = (newPosition.x - oldPosition.x) / CGFloat(animationSteps)
        let dy = (newPosition.y - oldPosition.y) / CGFloat(animationSteps)
        let dz = (newPosition.z - oldPosition.z) / CGFloat(animationSteps)
        
        flare.position = oldPosition
        delayLoop(animationDelay, steps: animationSteps) { i in
            flare.position = CGPoint(x: oldPosition.x + CGFloat(i) * dx,
                                     y: oldPosition.y + CGFloat(i) * dy,
                                     z: oldPosition.z + CGFloat(i) * dz)
            self.animate()
            if i == self.animationSteps - 1 { self.dataChanged() }
        }
    }
    
    func animateAngle(flare: Device, var oldAngle: Double, newAngle: Double) {
        if oldAngle == newAngle { return }
        
        // prevent the compass from spinning the wrong way
        if newAngle - oldAngle > 180.0 {
            oldAngle += 360.0
        } else if newAngle - oldAngle < -180.0 {
            oldAngle -= 360.0
        }
        
        let delta = (newAngle - oldAngle) / Double(animationSteps)
        
        flare.data["angle"] = oldAngle
        delayLoop(animationDelay, steps: animationSteps) { i in
            flare.data["angle"] = oldAngle + Double(i) * delta
            self.animate()
            if i == self.animationSteps - 1 { self.dataChanged() }
        }
    }
    
    func setNearbyThingData(key: String, value: AnyObject) {
        if nearbyThing != nil {
            nearbyThing!.data[key] = value
            flareManager.setData(nearbyThing!, key: key, value: value, sender: device)
            dataChanged()
        }
    }

    func registerDefaultsFromSettingsBundle() {
        defaults.synchronize()
        
        let settingsBundle: NSString = NSBundle.mainBundle().pathForResource("Settings", ofType: "bundle")!
        if(settingsBundle.containsString("")){
            NSLog("Could not find Settings.bundle");
            return;
        }
        let settings: NSDictionary = NSDictionary(contentsOfFile: settingsBundle.stringByAppendingPathComponent("Root.plist"))!
        let preferences: NSArray = settings.objectForKey("PreferenceSpecifiers") as! NSArray
        var defaultsToRegister = [String:AnyObject]()
        
        for prefSpecification in preferences {
            if (prefSpecification.objectForKey("Key") != nil) {
                let key: String = prefSpecification.objectForKey("Key") as! String
                if !key.containsString("") {
                    let currentObject: AnyObject? = defaults.objectForKey(key as String)
                    if currentObject == nil {
                        let objectToSet: AnyObject? = prefSpecification.objectForKey("DefaultValue")
                        defaultsToRegister[key] = objectToSet!
                    }
                }
            }
        }
        defaults.registerDefaults(defaultsToRegister)
        defaults.synchronize()
    }
    
    func sendMessage(message: JSONDictionary?) {
        if (session != nil && message != nil) {
            NSLog("Sending: \(message!)")
            session!.sendMessage(message!, replyHandler: nil, errorHandler: nil)
        }
    }
    
    func session(session: WCSession, didReceiveMessage incomingMessage: [String : AnyObject], replyHandler: ([String : AnyObject]) -> Void) {
        NSLog("Received message: \(incomingMessage)")

        var message: JSONDictionary? = nil
        if let get = incomingMessage["get"] as? String {
            
            NSLog("Received: \(incomingMessage)")
            
            if (get == "position") {
                message = positionMessage()
            } else if (get == "things") {
                message = thingsMessage()
            }
            
            if message != nil {
                NSLog("Replying: \(message!)")
                replyHandler(message!)
            }
        } else if let data = incomingMessage["data"] as? JSONDictionary,
            thingId = data["thing"] as? String,
            thing = flareManager.flareIndex[thingId] as? Thing,
            key = data["key"] as? String,
            value = data["value"] as? String
        {
            NSLog("Setting \(thing.name) \(key) \(value)")
            flareManager.setData(thing, key: key, value: value, sender: device)
        }
    }
    
    func thingsMessage() -> JSONDictionary? {
        if let zone = currentZone {
            return ["things": zone.things.map({$0.toJSON()})]
        } else {
            return nil
        }
    }
    
    func positionMessage() -> JSONDictionary? {
        if let position = device?.position {
            return ["position": position.toJSON()]
        } else {
            return nil
        }
    }
    


}

extension Thing {
    func imageName() -> String? {
        if let color = data["color"] as? String {
            return "\(name.lowercaseString)-\(color)"
        }
        return nil
    }
}

