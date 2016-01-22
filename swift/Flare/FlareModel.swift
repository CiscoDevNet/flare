//
//  Environment.swift
//  Flare Test
//
//  Created by Andrew Zamler-Carhart on 3/23/15.
//  Copyright (c) 2015 Andrew Zamler-Carhart. All rights reserved.
//

import Foundation
import CoreGraphics

public class Flare: NSObject {
    public var id: String
    public var name: String
    public var comment: String
    public var data: JSONDictionary
    public var actions: [String]
    public var created: NSDate
    public var modified: NSDate
    public var distance: Double? // the distance from the current position
    
    public init(json: JSONDictionary) {
        self.id = json.getString("_id")
        self.name = json.getString("name")
        self.comment = json.getString("description") // description is a property of NSObject
        self.data = json.getDictionary("data")
        self.actions = json.getStringArray("actions")
        self.created = json.getDate("created")
        self.modified = json.getDate("modified")
    }
    
    public var flareClass: String {
        let className = NSStringFromClass(self.dynamicType)
        return className.componentsSeparatedByString(".").last!
    }
    
    public override var description: String {
        return "\(self.flareClass) \(self.id) - \(self.name)"
    }

    public func setDistanceFrom(currentPosition: CGPoint) {
        // override to calculate the distance
    }
    
    public var flareInfo: JSONDictionary {
        var info = JSONDictionary()
        info[self.flareClass.lowercaseString] = self.id
        return info
    }
    
    public func parentId() -> String? {
        return nil
    }
    
    public func children() -> [Flare] {
        return Array()
    }
    
    public func childWithId(id: String) -> Flare? {
        for child in children() {
            if child.id == id {
                return child
            }
        }
        
        return nil
    }
    
    public func toJSON() -> JSONDictionary {
        var json = JSONDictionary()
        json["_id"] = self.id
        json["name"] = self.name
        json["description"] = self.comment
        json["data"] = self.data
        // json["created"] = self.created
        // json["modified"] = self.modified
        return json
    }
}

public protocol FlarePosition {
    var position: CGPoint { get set }
}

public protocol FlarePerimeter {
    var perimeter: CGRect { get set }
}

public class Environment: Flare, FlarePerimeter {
    public var geofence: Geofence
    public var perimeter: CGRect
    public var angle: Double
    public var uuid: String?

    public var zones = [Zone]()
    public var devices = [Device]()
    
    public class func loadJson(json: JSONDictionary) -> [Environment] {
        var results = [Environment]()
        for child in json.getArray("environments") {
            let environment = Environment(json: child)
            results.append(environment)
        }
        return results
    }
    
    public override init(json: JSONDictionary) {
        self.geofence = Geofence(json: json.getDictionary("geofence"))
        self.perimeter = getRect(json.getDictionary("perimeter"))
        self.angle = json.getDouble("angle")
        
        for child: JSONDictionary in json.getArray("zones") {
            let zone = Zone(json: child)
            zones.append(zone)
        }

        for child: JSONDictionary in json.getArray("devices") {
            let device = Device(json: child)
            devices.append(device)
        }

        super.init(json: json)
        
        if let uuid = self.data["uuid"] as? String { self.uuid = uuid }
    }
    
    public override var description: String {
        return "\(super.description) - \(perimeter)"
    }
    
    public override func children() -> [Flare] {
        return self.zones
    }
    
    public override func toJSON() -> JSONDictionary {
        var json = super.toJSON()
        json["geofence"] = self.geofence.toJSON()
        json["perimeter"] = self.perimeter.toJSON()
        json["angle"] = self.angle
        if zones.count > 0 {json["zones"] = self.zones.map({$0.toJSON()})}
        if devices.count > 0 {json["devices"] = self.devices.map({$0.toJSON()})}
        return json
    }

    public override func setDistanceFrom(latlong: CGPoint) {
        self.distance = self.geofence.distanceFrom(latlong)
    }

    public func here() -> Bool {
        return distance != nil && distance! * 1000 < self.geofence.radius
    }

    public func things() -> [Thing] {
        var results = [Thing]()
        for zone in zones {
            for thing in zone.things {
                results.append(thing)
            }
        }
        
        return results
    }

    public func beacons() -> [Int:Thing] {
        var results = [Int:Thing]()
        for zone in zones {
            if let major = zone.major {
                for thing in zone.things {
                    if let minor = thing.minor {
                        results[major * 10000 + minor] = thing
                        // NSLog("Beacon \(thing.name): \(zone.major!) \(thing.minor!)")
                    }
                }
            }
        }
        
        return results
    }
}

public class Zone: Flare, FlarePerimeter {
    public var environmentId: String
    
    public var perimeter: CGRect
    public var center: CGPoint
    public var major: Int?
    
    public var things = [Thing]()
    
    public override init(json: JSONDictionary) {
        self.environmentId = json.getString("environment")

        self.perimeter = getRect(json.getDictionary("perimeter"))
        self.center = self.perimeter.center()
        
        for child: JSONDictionary in json.getArray("things") {
            let thing = Thing(json: child)
            things.append(thing)
        }
        
        super.init(json: json)

        if let major = self.data["major"] as? Int { self.major = major }
    }
    
    public override var description: String {
        return "\(super.description) - \(perimeter)"
    }

    public override func parentId() -> String? {
        return environmentId
    }
    
    public override func children() -> [Flare] {
        return self.things
    }

    public override func toJSON() -> JSONDictionary {
        var json = super.toJSON()
        json["perimeter"] = self.perimeter.toJSON()
        if things.count > 0 {json["things"] = self.things.map({$0.toJSON()})}
        return json
    }

    public override func setDistanceFrom(currentPosition: CGPoint) {
        self.distance = perimeter.contains(currentPosition) ? 0.0 : Double(currentPosition - self.center)
    }
}

public class Thing: Flare, FlarePosition {
    public var environmentId: String
    public var zoneId: String
    
    public var type: String
    public var position: CGPoint
    public var minor: Int?
    
    public var distances = [Double]()
    public var inverseDistance = 0.0
    
    public override init(json: JSONDictionary) {
        self.environmentId = json.getString("environment")
        self.zoneId = json.getString("zone")
        
        self.type = json.getString("type")
        self.position = getPoint(json.getDictionary("position"))
        
        super.init(json: json)

        if let minor = self.data["minor"] as? Int { self.minor = minor }
    }
    
    public override var description: String {
        return "\(super.description) - \(position)"
    }

    public override func parentId() -> String? {
        return zoneId
    }

    public override func toJSON() -> JSONDictionary {
        var json = super.toJSON()
        json["position"] = self.position.toJSON()
        return json
    }
    
    public override func setDistanceFrom(currentPosition: CGPoint) {
        self.distance = Double(currentPosition - self.position)
    }

    public func addDistance(distance: Double) {
        distances.append(distance)
        while distances.count > 5 {
            distances.removeAtIndex(0)
        }
    }
    
    public func lastDistance() -> Double {
        if distances.count > 0 {
            return distances.last!
        }
        return -1
    }
    
    public func averageDistance() -> Double {
        var count = 0
        var total = 0.0
        
        for value in distances {
            if value != -1 {
                total += value
                count++
            }
        }
        
        if count == 0 {
            return -1
        }
        
        return total / Double(count)
    }
}

public class Device: Flare, FlarePosition {
    public var environmentId: String

    public var position: CGPoint
    
    public override init(json: JSONDictionary) {
        self.environmentId = json.getString("environment")

        self.position = getPoint(json.getDictionary("position"))
        
        super.init(json: json)
    }

    public override var description: String {
        return "\(super.description) - \(position)"
    }

    public override func parentId() -> String? {
        return environmentId
    }
    
    public override func toJSON() -> JSONDictionary {
        var json = super.toJSON()
        json["position"] = self.position.toJSON()
        return json
    }
    
    public func angle() -> Double {
        if let value = self.data["angle"] as? Double {
            return value
        } else {
            return 0
        }
    }
    
    public func distanceTo(thing: Thing) -> Double {
        return self.position - thing.position
    }
    
    public func angleTo(thing: Thing) -> Double {
        let dx = thing.position.x - self.position.x
        let dy = thing.position.y - self.position.y
        let radians = Double(atan2(dy, dx))
        var degrees = radiansToDegrees(radians)
        if degrees < 0 { degrees += 360.0 }
        return degrees
    }
}

public class Geofence: NSObject {
    public var latitude: Double
    public var longitude: Double
    public var radius: Double
    
    public init(json: JSONDictionary) {
        self.latitude = json.getDouble("latitude")
        self.longitude = json.getDouble("longitude")
        self.radius = json.getDouble("radius")
    }
    
    public override var description: String {
        let latLabel = self.latitude >= 0 ? "°N" : "°S"
        let longLabel = self.latitude >= 0 ? "°E" : "°W"
        return "\(self.latitude)\(latLabel), \(self.longitude)\(longLabel), \(self.radius)m))"
    }

    // calculates the distance in meters along the Earth's surface between the geofence and the given location
    public func distanceFrom(latlong: CGPoint) -> Double {
        let lat1rad = latitude * M_PI/180
        let lon1rad = longitude * M_PI/180
        let lat2rad = Double(latlong.x) * M_PI/180
        let lon2rad = Double(latlong.y) * M_PI/180
        
        let dLat = lat2rad - lat1rad
        let dLon = lon2rad - lon1rad
        let a = sin(dLat/2) * sin(dLat/2) + sin(dLon/2) * sin(dLon/2) * cos(lat1rad) * cos(lat2rad)
        let c = 2 * asin(sqrt(a))
        let R = 6372.8
        
        return R * c
    }
    
    public func toJSON() -> JSONDictionary {
        return ["latitude": self.latitude, "longitude": self.longitude, "radius": self.radius]
    }
}

public func getRect(json: JSONDictionary) -> CGRect {
    return CGRect(origin: getPoint(json.getDictionary("origin")), size: getSize(json.getDictionary("size")))
}

public func getPoint(json: JSONDictionary) -> CGPoint {
    return CGPoint(x: json.getDouble("x"), y: json.getDouble("y"))
}

public func getSize(json: JSONDictionary) -> CGSize {
    return CGSize(width: json.getDouble("width"), height: json.getDouble("height"))
}