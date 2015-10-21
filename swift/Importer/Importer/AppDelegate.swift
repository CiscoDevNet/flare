//
//  AppDelegate.swift
//  Importer
//
//  Created by Andrew Zamler-Carhart on 10/16/15.
//  Copyright (c) 2015 Cisco. All rights reserved.
//

import Cocoa
import Flare

@NSApplicationMain
class AppDelegate: NSObject, NSApplicationDelegate {

    @IBOutlet weak var window: NSWindow!

    let flareManager = FlareManager(host: "localhost", port: 1234)
    let filename = "model"
    
    func applicationDidFinishLaunching(aNotification: NSNotification) {
        importData()
    }

    func applicationWillTerminate(aNotification: NSNotification) {
        // Insert code here to tear down your application
    }

    func importData() {
        if let path = NSBundle.mainBundle().pathForResource(filename, ofType: "json"),
            contents = NSData(contentsOfFile: path),
            json = NSJSONSerialization.JSONObjectWithData(contents, options: nil, error: nil) as? JSONDictionary,
            environments = json["environments"] as? JSONArray
        {
            for environment in environments {
                self.importEnvironment(environment)
            }
        }
    }
    
    func importEnvironment(source: JSONDictionary) {
        var environment: JSONDictionary = [:]
        
        if let name = source["name"] as? String { environment["name"] = name }
        if let desc = source["description"] as? String { environment["description"] = desc }
        if let geofence = source["geofence"] as? JSONDictionary { environment["geofence"] = geofence }
        if let perimeter = source["perimeter"] as? JSONDictionary { environment["perimeter"] = perimeter }
        if let angle = source["angle"] as? Double { environment["angle"] = angle }
        if let data = source["data"] as? JSONDictionary { environment["data"] = data }
        
        flareManager.newEnvironment(environment) { result in
            if let environmentId = result["_id"] as? String,
                name = result["name"] as? String
            {
                NSLog("Imported environment \(name)")
            
                if let zones = source["zones"] as? JSONArray {
                    for zone in zones {
                        self.importZone(environmentId, source: zone)
                    }
                }

                if let devices = source["devices"] as? JSONArray {
                    for device in devices {
                        self.importDevice(environmentId, source: device)
                    }
                }
            }
        }
    }
    
    func importZone(environmentId: String, source: JSONDictionary) {
        var zone: JSONDictionary = [:]
        
        if let name = source["name"] as? String { zone["name"] = name }
        if let desc = source["description"] as? String { zone["description"] = desc }
        if let perimeter = source["perimeter"] as? JSONDictionary { zone["perimeter"] = perimeter }
        if let data = source["data"] as? JSONDictionary { zone["data"] = data }

        flareManager.newZone(environmentId, zone: zone) { result in
            if let zoneId = result["_id"] as? String,
                name = result["name"] as? String
            {
                NSLog("Imported zone \(name)")
                
                if let things = source["things"] as? JSONArray {
                    for thing in things {
                        self.importThing(environmentId, zoneId: zoneId, source: thing)
                    }
                }
            }
        }
    }
    
    func importThing(environmentId: String, zoneId: String, source: JSONDictionary) {
        var thing: JSONDictionary = [:]
        
        if let name = source["name"] as? String { thing["name"] = name }
        if let desc = source["description"] as? String { thing["description"] = desc }
        if let position = source["position"] as? JSONDictionary { thing["position"] = position }
        if let data = source["data"] as? JSONDictionary { thing["data"] = data }

        flareManager.newThing(environmentId, zoneId: zoneId, thing: thing) { result in
            if let name = result["name"] as? String {
                NSLog("Imported thing \(name)")
            }
        }
    }
    
    func importDevice(environmentId: String, source: JSONDictionary) {
        var device: JSONDictionary = [:]
        
        if let name = source["name"] as? String { device["name"] = name }
        if let desc = source["description"] as? String { device["description"] = desc }
        if let position = source["position"] as? JSONDictionary { device["position"] = position }
        if let data = source["data"] as? JSONDictionary { device["data"] = data }
        
        flareManager.newDevice(environmentId, device: device) { result in
            if let name = result["name"] as? String {
                NSLog("Imported device \(name)")
            }
        }
    }
}

