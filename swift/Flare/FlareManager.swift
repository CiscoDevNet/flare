//
//  FlareManager.swift
//  Trilateral
//
//  Created by Andrew Zamler-Carhart on 3/25/15.
//  Copyright (c) 2015 Cisco. All rights reserved.
//

import Foundation
import CoreGraphics
import SocketIO

@objc public protocol FlareManagerDelegate {
    optional func didReceiveData(flare: Flare, data: JSONDictionary, sender: Flare?)
    optional func didReceivePosition(flare: Flare, position: CGPoint, sender: Flare?)
    optional func handleAction(flare: Flare, action: String, sender: Flare?)
    optional func enter(zone: Zone, device: Device)
    optional func exit(zone: Zone, device: Device)
    optional func near(thing: Thing, device: Device, distance: Double)
    optional func far(thing: Thing, device: Device)
}

public class FlareManager: APIManager {
    
    public var debugSocket = true
    
    public var delegate: FlareManagerDelegate?
    public var socket: SocketIOClient
    public var flareIndex = [String:Flare]()
    
    public init(host: String, port: Int) {
        socket = SocketIOClient(socketURL: "\(host):\(port)")

        super.init()
        
        self.server = "http://\(host):\(port)" // TODO: support https and subpaths
    }
    
    public func connect() {
        self.addHandlers()
        self.socket.connect()
    }
    
    public func disconnect() {
        self.socket.disconnect()
    }
    
    // Asynchronously loads all zones and things for one environment, and then calls the handler.
    public func loadEnvironment(environment: Environment, handler:() -> ()) {
        var requests = 0
        
        requests++
        self.listZones(environment.id) {(jsonArray) -> () in
            for json in jsonArray {
                let zone = Zone(json: json)
                environment.zones.append(zone)
                self.addToIndex(zone)
                
                requests++
                self.listThings(environment.id, zoneId: zone.id) {(jsonArray) -> () in
                    for json in jsonArray {
                        let thing = Thing(json: json)
                        zone.things.append(thing)
                        self.addToIndex(thing)
                    }
                    
                    requests--
                    if requests == 0 { handler() }
                }
            }
            
            requests--
            if requests == 0 { handler() }
        }
    }
    
    // Asynchronously loads the complete environment / zone / thing / device hierarchy, and then calls the handler.
    public func loadEnvironments(handler:([Environment]) -> ()) {
        loadEnvironments(nil, loadDevices: true, handler: handler)
    }
    
    // Asynchronously loads the complete environment / zone / thing / device hierarchy, and then calls the handler.
    // params is optional and should contain latitude and longitude
    public func loadEnvironments(params: JSONDictionary?, loadDevices: Bool, handler:([Environment]) -> ()) {
        var environments = [Environment]()
        var requests = 0
        
        requests++
        self.listEnvironments(params) {(jsonArray) -> () in
            for json in jsonArray {
                let environment = Environment(json: json)
                environments.append(environment)
                self.addToIndex(environment)
                
                requests++
                self.listZones(environment.id) {(jsonArray) -> () in
                    for json in jsonArray {
                        let zone = Zone(json: json)
                        environment.zones.append(zone)
                        self.addToIndex(zone)
                        
                        requests++
                        self.listThings(environment.id, zoneId: zone.id) {(jsonArray) -> () in
                            for json in jsonArray {
                                let thing = Thing(json: json)
                                zone.things.append(thing)
                                self.addToIndex(thing)
                            }
                            
                            requests--
                            if requests == 0 { handler(environments) }
                        }
                    }
                    
                    requests--
                    if requests == 0 { handler(environments) }
                }
                
                if loadDevices {
                    requests++
                    self.listDevices(environment.id) {(jsonArray) -> () in
                        for json in jsonArray {
                            let device = Device(json: json)
                            environment.devices.append(device)
                            self.addToIndex(device)
                        }
                        
                        requests--
                        if requests == 0 { handler(environments) }
                    }
                }
            }
            
            requests--
            if requests == 0 { handler(environments) }
        }
    }

    // used to safely modify a Flare object on the server
    // the handler takes the current JSON as input, and should return the modified JSON as output
    public func modifyFlare(flare: Flare, handler:(JSONDictionary) -> (JSONDictionary)) {
        getFlare(flare) {json in
            NSLog("Current  \(flare): \(json)")
            let modifiedJson = handler(json)
            NSLog("Modified \(flare): \(modifiedJson)")
            self.updateFlare(flare, json: modifiedJson) {json in }
        }
    }
    
    // gets the up-to-date JSON for a Flare object from the server
    public func getFlare(flare: Flare, handler:(JSONDictionary) -> ()) {
        if let environment = flare as? Environment {
            getEnvironment(environment.id, handler: handler)
        } else if let zone = flare as? Zone {
            getZone(zone.id, environmentId: zone.environmentId, handler: handler)
        } else if let thing = flare as? Thing {
            getThing(thing.id, environmentId: thing.environmentId, zoneId: thing.zoneId, handler: handler)
        } else if let device = flare as? Device {
            getDevice(device.id, environmentId: device.environmentId, handler: handler)
        }
    }
    
    // creates a new Flare object on the server
    // if the flare is nil, creates an environment
    // if the flare is an environment, creates a zone
    // if the flare is a zone, creates a thing
    public func newFlare(flare: Flare?, json: JSONDictionary, handler:(JSONDictionary) -> ()) {
        var template = json
        if flare == nil {
             template["perimeter"] = ["origin":["x":0, "y":0], "size":["width":10, "height":10]]
            newEnvironment( template, handler: handler)
        } else if let environment = flare as? Environment {
            template["perimeter"] = ["origin":["x":0, "y":0], "size":["width":5, "height":5]]
            newZone(environment.id, zone:  template, handler: handler)
        } else if let zone = flare as? Zone {
            template["position"] = ["x":0, "y":0]
            newThing(zone.environmentId, zoneId: zone.id, thing:  template, handler: handler)
        }
    }
    
    // updates the JSON for a Flare object on the server
    public func updateFlare(flare: Flare, json: JSONDictionary, handler:(JSONDictionary) -> ()) {
        if let environment = flare as? Environment {
            updateEnvironment(environment.id, environment: json, handler: handler)
        } else if let zone = flare as? Zone {
            updateZone(zone.id, environmentId: zone.environmentId, zone: json, handler: handler)
        } else if let thing = flare as? Thing {
            updateThing(thing.id, environmentId: thing.environmentId, zoneId: thing.zoneId, thing: json, handler: handler)
        } else if let device = flare as? Device {
            updateDevice(device.id, environmentId: device.environmentId, device: json, handler: handler)
        }
    }
    
    // deletes a Flare object on the server
    public func deleteFlare(flare: Flare, handler:(JSONDictionary) -> ()) {
        if let environment = flare as? Environment {
            deleteEnvironment(environment.id, handler: handler)
        } else if let zone = flare as? Zone {
            deleteZone(zone.id, environmentId: zone.environmentId, handler: handler)
        } else if let thing = flare as? Thing {
            deleteThing(thing.id, environmentId: thing.environmentId, zoneId: thing.zoneId, handler: handler)
        } else if let device = flare as? Device {
            deleteDevice(device.id, environmentId: device.environmentId, handler: handler)
        }
    }
    
    public func addToIndex(flare: Flare) {
        self.flareIndex[flare.id] = flare
    }
    
    public func flareWithName(array: [Flare], name: String) -> Flare? {
        for flare in array {
            if flare.name == name {
                return flare
            }
        }
        return nil
    }
    
    public func flareForMessage(message: JSONDictionary) -> Flare? {
        if let id = message["thing"] as? String {
            return flareIndex[id];
        } else if let id = message["device"] as? String {
            return flareIndex[id];
        } else if let id = message["zone"] as? String {
            return flareIndex[id];
        } else if let id = message["environment"] as? String {
            return flareIndex[id];
        } else {
            return nil
        }
    }
    
    public func environmentForFlare(flare: Flare) -> Environment? {
        if let environment = flare as? Environment {
            return environment
        } else if let zone = flare as? Zone, environment = flareIndex[zone.environmentId] as? Environment {
            return environment
        } else if let thing = flare as? Thing, environment = flareIndex[thing.environmentId] as? Environment {
            return environment
        } else if let device = flare as? Device, environment = flareIndex[device.environmentId] as? Environment {
            return environment
        } else {
            return nil
        }
    }
    
    // MARK: Environments
    
    public func listEnvironments(handler:(JSONArray) -> ()) {
        sendRequest("environments")
            {json in handler(json as! JSONArray)}
    }
    
    // return environments filtered by parameters
    // latitude, longitude: filter environments whose geofence contains the given point
    // key, value: filter environments whose data contains the given key/value pair
    public func listEnvironments(params: JSONDictionary?, handler:(JSONArray) -> ()) {
        sendRequest("environments", params: params)
            {json in handler(json as! JSONArray)}
    }

    public func newEnvironment(environment: JSONDictionary, handler:(JSONDictionary) -> ()) {
        sendRequest("environments", params: nil, method: .POST, message: environment)
            {json in handler(json as! JSONDictionary)}
    }
    
    public func getEnvironment(environmentId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)")
            {json in handler(json as! JSONDictionary)}
    }
    
    public func updateEnvironment(environmentId: String, environment: JSONDictionary, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)", params: nil, method: .PUT, message: environment)
            {json in handler(json as! JSONDictionary)}
    }
    
    public func deleteEnvironment(environmentId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)", params: nil, method: .DELETE, message: nil)
            {json in handler(json as! JSONDictionary)}
    }
    
    // MARK: Zones
    
    public func listZones(environmentId: String, handler:(JSONArray) -> ()) {
        sendRequest("environments/\(environmentId)/zones")
            {json in handler(json as! JSONArray)}
    }
    
    // return only zones in the environment containing the given point
    public func listZones(environmentId: String, point: CGPoint, handler:(JSONArray) -> ()) {
        let params = ["x":"\(point.x)", "y":"\(point.x)"]
        listZones(environmentId, params: params, handler: handler)
    }
    
    // return zones filtered by parameters
    // x, y: filter zones whose perimeter contains the given point
    // key, value: filter zones whose data contains the given key/value pair
    public func listZones(environmentId: String, params: JSONDictionary?, handler:(JSONArray) -> ()) {
        sendRequest("environments/\(environmentId)/zones", params: params)
            {json in handler(json as! JSONArray)}
    }
    
    public func newZone(environmentId: String, zone: JSONDictionary, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/zones", params: nil, method: .POST, message: zone)
            {json in handler(json as! JSONDictionary)}
    }
    
    public func getZone(zoneId: String, environmentId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)")
            {json in handler(json as! JSONDictionary)}
    }
    
    public func updateZone(zoneId: String, environmentId: String, zone: JSONDictionary, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)", params: nil, method: .PUT, message: zone)
            {json in handler(json as! JSONDictionary)}
    }
    
    public func deleteZone(zoneId: String, environmentId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)", params: nil, method: .DELETE, message: nil)
            {json in handler(json as! JSONDictionary)}
    }
    
    // MARK: Things
    
    public func listThings(environmentId: String, zoneId: String, handler:(JSONArray) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)/things")
            {json in handler(json as! JSONArray)}
    }
    
    // return things filtered by parameters
    // x, y, distance: filter things whose position is within distance from the given point
    // key, value: filter things whose data contains the given key/value pair
    public func listThings(environmentId: String, zoneId: String, params: JSONDictionary?, handler:(JSONArray) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)/things", params: params)
            {json in handler(json as! JSONArray)}
    }
    
    public func newThing(environmentId: String, zoneId: String, thing: JSONDictionary, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)/things", params: nil, method: .POST, message: thing)
            {json in handler(json as! JSONDictionary)}
    }
    
    public func getThing(thingId: String, environmentId: String, zoneId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)/things/\(thingId)")
            {json in handler(json as! JSONDictionary)}
    }
    
    public func getThingData(thingId: String, environmentId: String, zoneId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)/things/\(thingId)/data")
            {json in handler(json as! JSONDictionary)}
    }

    public func getThingDataValue(thingId: String, environmentId: String, zoneId: String, key: String, handler:(AnyObject) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)/things/\(thingId)/data/\(key)", handler: handler)
    }
    
    public func getThingPosition(thingId: String, environmentId: String, zoneId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)/things/\(thingId)/position")
            {json in handler(json as! JSONDictionary)}
    }
    
    public func updateThing(thingId: String, environmentId: String, zoneId: String, thing: JSONDictionary, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)/things/\(thingId)", params: nil, method: .PUT, message: thing)
            {json in handler(json as! JSONDictionary)}
    }
    
    public func deleteThing(thingId: String, environmentId: String, zoneId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/zones/\(zoneId)/things/\(thingId)", params: nil, method: .DELETE, message: nil)
            {json in handler(json as! JSONDictionary)}
    }
    
    // MARK: User Devices
    
    public func listDevices(environmentId: String, handler:(JSONArray) -> ()) {
        sendRequest("environments/\(environmentId)/devices")
            {json in handler(json as! JSONArray)}
    }
    
    // return things filtered by parameters
    // x, y, distance: filter things whose position is within distance from the given point
    // key, value: filter things whose data contains the given key/value pair
    public func listDevices(environmentId: String, params: JSONDictionary?, handler:(JSONArray) -> ()) {
        sendRequest("environments/\(environmentId)/devices", params: params)
            {json in handler(json as! JSONArray)}
    }

    public func newDevice(environmentId: String, device: JSONDictionary, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/devices", params: nil, method: .POST, message: device)
            {json in handler(json as! JSONDictionary)}
    }
    
    public func getDevice(deviceId: String, environmentId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/devices/\(deviceId)")
            {json in handler(json as! JSONDictionary)}
    }
    
    public func getDeviceData(deviceId: String, environmentId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/devices/\(deviceId)/data")
            {json in handler(json as! JSONDictionary)}
    }
    
    public func getDeviceDataValue(deviceId: String, environmentId: String, key: String, handler:(AnyObject) -> ()) {
        sendRequest("environments/\(environmentId)/devices/\(deviceId)/data/\(key)", handler: handler)
    }
    
    public func getDevicePosition(deviceId: String, environmentId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/devices/\(deviceId)/position")
            {json in handler(json as! JSONDictionary)}
    }
    
    public func updateDevice(deviceId: String, environmentId: String, device: JSONDictionary, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/devices/\(deviceId)", params: nil, method: .PUT, message: device)
            {json in handler(json as! JSONDictionary)}
    }
    
    public func deleteDevice(deviceId: String, environmentId: String, handler:(JSONDictionary) -> ()) {
        sendRequest("environments/\(environmentId)/devices/\(deviceId)", params: nil, method: .DELETE, message: nil)
            {json in handler(json as! JSONDictionary)}
    }
    
    // tries to find an existing device object in the current environment
    // if one is not found, creates a new device object
    public func getCurrentDevice(environmentId: String, template: JSONDictionary, handler: (Device?) -> ()) {
        self.savedDevice(environmentId) { (device) -> () in
            if device != nil {
                handler(device)
            } else {
                self.newDeviceObject(environmentId, template: template) { (device) -> () in
                    if device != nil {
                        handler(device)
                    }
                }
            }
        }
    }
    
    // looks for an existing device object in the current environment, and if found calls the handler with it
    public func savedDevice(environmentId: String, handler: (Device?) -> ()) {
        if let deviceId = NSUserDefaults.standardUserDefaults().stringForKey("deviceId") {
            self.getDevice(deviceId, environmentId: environmentId) { (json) -> () in
                if let _ = json["_id"] as? String {
                    if let deviceEnvironment = json["environment"] as? String {
                        if deviceEnvironment == environmentId {
                            let device = Device(json: json)
                            self.addToIndex(device)
                            
                            NSLog("Found existing device: \(device.name)")
                            handler(device)
                        } else {
                            // NSLog("Device in wrong environment")
                            handler(nil)
                        }
                    } else {
                        // NSLog("Device has no environment")
                        handler(nil)
                    }
                } else {
                    // NSLog("Device not found")
                    handler(nil)
                }
            }
        } else {
            // NSLog("No saved device")
            handler(nil)
        }
    }
    
    // creates a new device object using the default values in the template
    public func newDeviceObject(environmentId: String, template: JSONDictionary, handler: (Device?) -> ()) {
        newDevice(environmentId, device: template) { (json) -> () in
            let device = Device(json: json)
            self.addToIndex(device)
            
            NSUserDefaults.standardUserDefaults().setObject(device.id, forKey: "deviceId")
            NSLog("Created new device: \(device.name)")
            handler(device)
        }
    }
    
    // MARK: SocketIO sent
    
    public func emit(event: String, message: JSONDictionary) {
        if debugSocket { NSLog("\(event): \(message)") }
        socket.emit(event, message)
    }
    
    public func subscribe(flare: Flare) {
        subscribe(flare, all: false)
    }
    
    public func subscribe(flare: Flare, all: Bool) {
        var message = flare.flareInfo
        if all { message["all"] = true }
        emit("subscribe", message: message)
    }
    
    public func unsubscribe(flare: Flare) {
        let message = flare.flareInfo
        emit("unsubscribe", message: message)
    }
    
    public func getData(flare: Flare) {
        let message = flare.flareInfo
        emit("getData", message: message)
    }
    
    /// Gets one key/value pair of data for an object
    public func getData(flare: Flare, key: String) {
        var message = flare.flareInfo
        message["key"] = key
        emit("getData", message: message)
    }
    
    public func setData(flare: Flare, key: String, value: AnyObject, sender: Flare?) {
        var message = flare.flareInfo
        message["key"] = key
        message["value"] = value
        if sender != nil { message["sender"] = sender!.id }
        emit("setData", message: message)
    }
    
    public func getPosition(flare: Flare) {
        let message = flare.flareInfo
        emit("getPosition", message: message)
    }
    
    public func setPosition(flare: Flare, position: CGPoint, sender: Flare?) {
        var message = flare.flareInfo
        if position.x.isNaN || position.y.isNaN {
            NSLog("Invalid position: \(position)")
            return
        }
        message["position"] = ["x":position.x, "y":position.y]
        if sender != nil { message["sender"] = sender!.id }
        emit("setPosition", message: message)
    }
    
    public func performAction(flare: Flare, action: String, sender: Flare?) {
        var message = flare.flareInfo
        message["action"] = action
        if sender != nil { message["sender"] = sender!.id }
        emit("performAction", message: message)
    }
    
    // MARK: SocketIO received
    
    public func addHandlers() {
        socket.on("data") {messages, ack in
            if let message = messages[0] as? JSONDictionary,
                flare = self.flareForMessage(message),
                data = message["data"] as? JSONDictionary
            {
                if self.debugSocket { NSLog("data: \(message)") }
                for (key,value) in data {
                    flare.data[key] = value
                }
                
                var sender: Flare? = nil
                if let senderId = message["sender"] as? String {
                    sender = self.flareIndex[senderId]
                }
                
                self.delegate?.didReceiveData?(flare, data: data, sender: sender)
            }
        }
        
        socket.on("position") {messages, ack in
            if let message = messages[0] as? JSONDictionary,
                flare = self.flareForMessage(message),
                positionDict = message["position"] as? JSONDictionary
            {
                if self.debugSocket { NSLog("position: \(message)") }
                let position = getPoint(positionDict);
                
                if let thing = flare as? Thing {
                    thing.position = position
                } else if let device = flare as? Device {
                    device.position = position
                }
                
                var sender: Flare? = nil
                if let senderId = message["sender"] as? String {
                    sender = self.flareIndex[senderId]
                }
                
                self.delegate?.didReceivePosition?(flare, position: position, sender: sender)
            }
        }
        
        socket.on("handleAction") {messages, ack in
            if let message = messages[0] as? JSONDictionary,
                flare = self.flareForMessage(message),
                action = message["action"] as? String
            {
                if self.debugSocket { NSLog("handleAction: \(message)") }

                var sender: Flare? = nil
                if let senderId = message["sender"] as? String {
                    sender = self.flareIndex[senderId]
                }
                
                self.delegate?.handleAction?(flare, action: action, sender: sender)
            }
        }
        
        socket.on("enter") {messages, ack in
            if let message = messages[0] as? JSONDictionary,
                zoneId = message["zone"] as? String,
                deviceId = message["device"] as? String,
                zone = self.flareIndex[zoneId] as? Zone,
                device = self.flareIndex[deviceId] as? Device
            {
                if self.debugSocket { NSLog("enter: \(message)") }
                self.delegate?.enter?(zone, device: device)
            }
        }
        
        socket.on("exit") {messages, ack in
            if let message = messages[0] as? JSONDictionary,
                zoneId = message["zone"] as? String,
                deviceId = message["device"] as? String,
                zone = self.flareIndex[zoneId] as? Zone,
                device = self.flareIndex[deviceId] as? Device
            {
                if self.debugSocket { NSLog("exit: \(message)") }
                self.delegate?.exit?(zone, device: device)
            }
        }
        
        socket.on("near") {messages, ack in
            if let message = messages[0] as? JSONDictionary,
                thingId = message["thing"] as? String,
                deviceId = message["device"] as? String,
                thing = self.flareIndex[thingId] as? Thing,
                device = self.flareIndex[deviceId] as? Device,
                distance = message["distance"] as? Double
            {
                if self.debugSocket { NSLog("near: \(message)") }
                self.delegate?.near?(thing, device: device, distance: distance)
            }
        }
        
        socket.on("far") {messages, ack in
            if let message = messages[0] as? JSONDictionary,
                thingId = message["thing"] as? String,
                deviceId = message["device"] as? String,
                thing = self.flareIndex[thingId] as? Thing,
                device = self.flareIndex[deviceId] as? Device
            {
                if self.debugSocket { NSLog("far: \(message)") }
                self.delegate?.far?(thing, device: device)
            }
        }
    }
    
}
