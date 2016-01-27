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

// Each of the UIView(Controller)s that control the various tabs conforms to this protocol.
// When the current objects change these variables will be set, and when the objects' data
// changes the dataChanged() function will be called.
protocol FlareController {
    var currentEnvironment: Environment? { get set }
    var currentZone: Zone? { get set }
    var device: Device? { get set }
    var nearbyThing: Thing? { get set }
    
    func dataChanged()
}

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate, CLLocationManagerDelegate, FlareManagerDelegate, BeaconManagerDelegate {

    var window: UIWindow?

    var host = "localhost"
    var port = 1234
    
    let animationDelay = 0.5 // the duration of the animation in seconds
    let animationSteps = 30 // the number of times during the animation that the display is updated
    
    var defaults = NSUserDefaults.standardUserDefaults()
    var flareManager = FlareManager(host: "localhost", port: 1234)
    var beaconManager = BeaconManager()
    var currentLatlong: CLLocation?
    
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
    
    func application(application: UIApplication, didFinishLaunchingWithOptions launchOptions: [NSObject: AnyObject]?) -> Bool {
        registerDefaultsFromSettingsBundle()

        if let newHost = defaults.stringForKey("host") { host = newHost }
        let newPort = defaults.integerForKey("port")
        if newPort != 0 { port = newPort }
        
        NSLog("Server: \(host):\(port)")
        NSLog("GPS: \(defaults.boolForKey("useGPS") ? "on" : "off")")
        NSLog("Beacons: \(defaults.boolForKey("useBeacons") ? "on" : "off")")
        NSLog("Compass: \(defaults.boolForKey("useCompass") ? "on" : "off")")
        
        flareManager = FlareManager(host: host, port: port)
        flareManager.debugSocket = false // turn on to print all Socket.IO messages
        
        flareManager.delegate = self
        beaconManager.delegate = self
        
        flareManager.connect()

        if !defaults.boolForKey("useGPS") { loadDefaultEnvironment() }
        
        return true
    }

    // called at startup, and when the GPS location changes significantly
    func deviceLocationDidChange(location: CLLocation) {
        NSLog("Location: \(location.coordinate.latitude),\(location.coordinate.longitude)")
        let params = ["latitude":location.coordinate.latitude, "longitude":location.coordinate.longitude]
        
        loadEnvironments(params)
    }
    
    func loadEnvironments(params: JSONDictionary?) {
        self.flareManager.loadEnvironments(params, loadDevices: false) { (environments) -> () in // load environment for current location
            if environments.count > 0 {
                self.loadEnvironment(environments[0])
            } else {
                self.flareManager.loadEnvironments(nil, loadDevices: false) { (environments) -> () in // load all environments
                    if environments.count > 0 {
                        NSLog("No environments found nearby, using first one.")
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
        NSLog("Current environment: \(self.currentEnvironment!.name)")
        
        self.flareManager.getCurrentDevice(self.currentEnvironment!.id, template: self.deviceTemplate()) { (device) -> () in
            self.loadDevice(device)
        }
        
        self.beaconManager.loadEnvironment(self.currentEnvironment!)
        if defaults.boolForKey("useBeacons") { self.beaconManager.start() }
        if defaults.boolForKey("useGPS") { self.beaconManager.startMonitoringLocation() }
        if defaults.boolForKey("useCompass") { self.beaconManager.startUpdatingHeading() }
        
        self.updateFlareController()

    }
    
    func loadDevice(value: Device?) {
        if let device = value {
            self.device = device
            self.flareManager.subscribe(device)
            
            loadCurrentZone()
            loadNearbyThing()
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
        flareManager.disconnect()
    }

    func applicationDidEnterBackground(application: UIApplication) {

    }

    func applicationWillEnterForeground(application: UIApplication) {
    
    }

    func applicationDidBecomeActive(application: UIApplication) {
        if defaults.boolForKey("useBeacons") { beaconManager.start() }
        if defaults.boolForKey("useGPS") { beaconManager.startMonitoringLocation() }
        if defaults.boolForKey("useCompass") { beaconManager.startUpdatingHeading() }
        flareManager.connect()
    }

    func applicationWillTerminate(application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }

    func devicePositionDidChange(position: CGPoint) {
        if device != nil {
            animateFlare(device!, oldPosition: device!.position, newPosition: position)
            flareManager.setPosition(device!, position: position, sender: nil)
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
        NSLog("\(flare.name) position: \(newPosition)")
        animateFlare(flare as! FlarePosition, oldPosition: oldPosition, newPosition: newPosition)
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
            
            flareManager.subscribe(thing)
            flareManager.getData(thing)
            flareManager.getPosition(thing)
            
        }
    }
    
    func far(thing: Thing, device: Device) {
        NSLog("far: \(thing.name)")
        
        if device == self.device && thing == self.nearbyThing {
            
            flareManager.unsubscribe(thing)
            
            nearbyThing = nil
        }
    }

    func animateFlare(var flare: FlarePosition, oldPosition: CGPoint, newPosition: CGPoint) {
        if oldPosition == newPosition { return }
        
        let dx = (newPosition.x - oldPosition.x) / CGFloat(animationSteps)
        let dy = (newPosition.y - oldPosition.y) / CGFloat(animationSteps)
        
        flare.position = oldPosition
        delayLoop(animationDelay, steps: animationSteps) { i in
            flare.position = CGPoint(x: oldPosition.x + CGFloat(i) * dx,
                y: oldPosition.y + CGFloat(i) * dy)
            self.dataChanged()
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
            self.dataChanged()
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
}

