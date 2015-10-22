//
//  ViewController.swift
//  Trilateral
//
//  Created by Andrew Zamler-Carhart on 3/24/15.
//  Copyright (c) 2015 Cisco. All rights reserved.
//

import UIKit
import Flare
import CoreLocation

class ViewController: UIViewController, CLLocationManagerDelegate, FlareManagerDelegate, BeaconManagerDelegate {

    var host = "localhost"
    var port = 1234
    
    var defaults = NSUserDefaults.standardUserDefaults()
    var flareManager: FlareManager
    var beaconManager = BeaconManager()
    var currentLatlong: CLLocation?
    
    var currentEnvironment: Environment?
    var currentZone: Zone?
    var device: Device?
    var nearbyThing: Thing?
    
    @IBOutlet weak var map: IndoorMap!
    
    @IBOutlet weak var instant1: UILabel!
    @IBOutlet weak var instant2: UILabel!
    @IBOutlet weak var instant3: UILabel!
    
    @IBOutlet weak var average1: UILabel!
    @IBOutlet weak var average2: UILabel!
    @IBOutlet weak var average3: UILabel!
    
    @IBOutlet weak var averageX: UILabel!
    @IBOutlet weak var averageY: UILabel!
    
    @IBOutlet weak var instantX: UILabel!
    @IBOutlet weak var instantY: UILabel!
    
    required init?(coder aDecoder: NSCoder) {
        if let newHost = defaults.stringForKey("host") { host = newHost }
        let newPort = defaults.integerForKey("port")
        if newPort != 0 { port = newPort }

        flareManager = FlareManager(host: host, port: port)
        
        super.init(coder: aDecoder)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        flareManager.delegate = self
        beaconManager.delegate = self
        
        flareManager.connect()
        
    }

    // called at startup, and when the GPS location changes significantly 
    func deviceLocationDidChange(location: CLLocation) {
        NSLog("Location: \(location.coordinate.latitude),\(location.coordinate.longitude)")
        let params = ["latitude":location.coordinate.latitude, "longitude":location.coordinate.longitude]

        flareManager.loadEnvironments(params, loadDevices: false) { (environments) -> () in
            if environments.count > 0 {
                self.currentEnvironment = environments[0]
                NSLog("Current environment: \(self.currentEnvironment!.name)")

                self.flareManager.getCurrentDevice(self.currentEnvironment!.id, template: self.deviceTemplate()) { (device) -> () in
                    self.loadDevice(device)
                }
                
                self.beaconManager.loadEnvironment(self.currentEnvironment!)
                self.map.loadEnvironment(self.currentEnvironment!)
                
                self.beaconManager.start()
                self.beaconManager.startMonitoringLocation()
            } else {
                NSLog("No environments found nearby.")
            }
        }
    }
    
    // returns a template used for creating new device objects:
    // name: Andrew's iPhone
    // description: iPhone, iOS 9.0
    // data: {}
    // postion: {"x":0, "y":0}
    func deviceTemplate() -> JSONDictionary {
        let uidevice = UIDevice.currentDevice()
        let name = uidevice.name
        let description = "\(uidevice.model), iOS \(uidevice.systemVersion)"
        return ["name":name, "description":description, "data":JSONDictionary(), "position":["x":0, "y":0]]
    }
    
    func loadDevice(value: Device?) {
        if let device = value {
            self.device = device
            self.flareManager.subscribe(device)
            self.flareManager.setPosition(device, position: CGPoint(x:-10.0, y:-10.0), sender: nil)
            self.map.device = device
        }
    }
    
    override func viewDidAppear(animated: Bool) {
        beaconManager.start()
        beaconManager.startMonitoringLocation()
        flareManager.connect()
    }
    
    override func viewDidDisappear(animated: Bool) {
        beaconManager.stop()
        beaconManager.stopMonitoringLocation()
        flareManager.disconnect()
    }
    
    func devicePositionDidChange(position: CGPoint) {
        if device != nil {
            device!.position = position
            map.dataChanged()
            flareManager.setPosition(device!, position: position, sender: nil)
        }
    }
    
    func didReceiveData(flare: Flare, data: JSONDictionary, sender: Flare?) {
        NSLog("\(flare.name) data: \(data)")
        
        if flare == device {
            map.dataChanged()
        } else if flare == nearbyThing {
            map.dataChanged()
        } else {
            map.dataChanged()
        }
    }
    
    func didReceivePosition(flare: Flare, position: CGPoint, sender: Flare?) {
        NSLog("\(flare.name) position: \(position)")
        
        if flare == device {
            map.dataChanged()
        } else if flare == nearbyThing {
            map.dataChanged()
        } else {
            map.dataChanged()
        }
    }
    
    func enter(zone: Zone, device: Device) {
        NSLog("\(zone.name) enter: \(device.name)")
        self.currentZone = zone
    }
    
    func exit(zone: Zone, device: Device) {
        NSLog("\(zone.name) exit: \(device.name)")
        self.currentZone = nil
    }
    
    func near(thing: Thing, device: Device, distance: Double) {
        NSLog("near: \(thing.name)")
        
        if device == self.device && thing != self.nearbyThing {
            nearbyThing = thing
            map.nearbyThing = thing
            
            // flareManager.subscribe(thing)
            flareManager.getData(thing)
            flareManager.getPosition(thing)
            
        }
    }
    
    func far(thing: Thing, device: Device) {
        NSLog("far: \(thing.name)")
        
        if device == self.device && thing == self.nearbyThing {
            
            // flareManager.unsubscribe(thing)
            
            nearbyThing = nil
            map.nearbyThing = nil
        }
    }

    @IBAction func rainbow() {
        if nearbyThing != nil {
            flareManager.performAction(nearbyThing!, action: "rainbow", sender: device)
        }
    }
    
    @IBAction func invert() {
        if nearbyThing != nil {
            flareManager.performAction(nearbyThing!, action: "invert", sender: device)
        }
    }
    
    @IBAction func lighter() {
        if nearbyThing != nil {
            flareManager.performAction(nearbyThing!, action: "lighter", sender: device)
        }
    }
    
    @IBAction func darker() {
        if nearbyThing != nil {
            flareManager.performAction(nearbyThing!, action: "darker", sender: device)
        }
    }
    
    func updateDistances(minor: Int, instant: UILabel, average: UILabel) {
        if let beacon = beaconManager.beacons[minor] {
            if let label = map.labels[beacon.id] {
                label.text = String(format:"\(beacon.name) (%.2f)", beacon.averageDistance())
            }
        }
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }


}
