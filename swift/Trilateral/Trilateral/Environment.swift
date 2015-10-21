//
//  Environment.swift
//  Flare Test
//
//  Created by Andrew Zamler-Carhart on 3/23/15.
//  Copyright (c) 2015 Andrew Zamler-Carhart. All rights reserved.
//

import Foundation
import CoreGraphics

class Flare: NSObject {
    var id: String
    var name: String
    var comment: String
    var data: JSONDictionary
    var distance: Double? // the distance from the current position
    
    init(json: JSONDictionary) {
        self.id = json.getString("id")
        self.name = json.getString("name")
        self.comment = json.getString("description") // description is a property of NSObject
        self.data = json.getDictionary("data")
    }

    override var description: String {
        let className = self.dynamicType
        return "\(className) \(self.id) - \(self.name)"
    }
    
    func setDistanceFrom(currentPosition: CGPoint) {
        // override to calculate the distance
    }
}

class Environment: Flare {
    var geofence: Geofence
    var perimeter: CGRect
    var angle: Double

    var zones = [Zone]()
    
    class func getAll(json: JSONDictionary) -> [Environment] {
        var results = [Environment]()
        for child in json.getArray("environments") {
            let environment = Environment(json: child)
            results.append(environment)
        }
        return results
    }
    
    override init(json: JSONDictionary) {
        self.geofence = Geofence(json: json.getDictionary("geofence"))
        self.perimeter = getRect(json.getDictionary("perimeter"))
        self.angle = json.getDouble("angle")
        
        for child: JSONDictionary in json.getArray("zones") {
            let zone = Zone(json: child)
            zones.append(zone)
        }
        
        super.init(json: json)
    }
    
    override var description: String {
        return "\(super.description) - \(perimeter)"
    }
    
    override func setDistanceFrom(latlong: CGPoint) {
        self.distance = self.geofence.distanceFrom(latlong)
    }
    
    func here() -> Bool {
        return distance != nil && distance! * 1000 < self.geofence.radius
    }
}

class Zone: Flare {
    var perimeter: CGRect
    var center: CGPoint
    var uuid: String?
    var major: Int?
    
    var things = [Thing]()
    
    override init(json: JSONDictionary) {
        self.perimeter = getRect(json.getDictionary("perimeter"))
        self.center = self.perimeter.center()
        
        for child: JSONDictionary in json.getArray("things") {
            let thing = Thing(json: child)
            things.append(thing)
        }
        
        super.init(json: json)

        self.uuid = self.data["uuid"] as! String?
        self.major = self.data["major"] as! Int?
    }
    
    override var description: String {
        return "\(super.description) - \(perimeter)"
    }
    
    func beacons() -> [Int:Thing] {
        var results = [Int:Thing]()
        for thing in things {
            if let minor = thing.minor {
                results[minor] = thing
            }
        }
        return results
    }
    
    override func setDistanceFrom(currentPosition: CGPoint) {
        self.distance = perimeter.contains(currentPosition) ? 0.0 : Double(currentPosition - self.center)
    }
}

class Thing: Flare {
    var type: String
    var position: CGPoint
    var angle: Double
    var minor: Int?
    
    var distances = [Double]()
    var inverseDistance = 0.0
    
    override init(json: JSONDictionary) {
        self.type = json.getString("type")
        self.position = getPoint(json.getDictionary("position"))
        self.angle = json.getDouble("angle") // defaults to 0°
        
        super.init(json: json)

        self.minor = self.data["minor"] as! Int?
    }
    
    override var description: String {
        return "\(super.description) - \(position)"
    }
    
    func addDistance(distance: Double) {
        distances.append(distance)
        while distances.count > 5 {
            distances.removeAtIndex(0)
        }
    }

    func lastDistance() -> Double {
        if distances.count > 0 {
            return distances.last!
        }
        return -1
    }
    
    func averageDistance() -> Double {
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
    
    override func setDistanceFrom(currentPosition: CGPoint) {
        self.distance = Double(currentPosition - self.position)
    }
}

class Geofence: NSObject {
    var latitude: Double
    var longitude: Double
    var radius: Double
    
    init(json: JSONDictionary) {
        self.latitude = json.getDouble("latitude")
        self.longitude = json.getDouble("longitude")
        self.radius = json.getDouble("radius")
    }
    
    override var description: String {
        let latLabel = self.latitude >= 0 ? "°N" : "°S"
        let longLabel = self.latitude >= 0 ? "°E" : "°W"
        return "\(self.latitude)\(latLabel), \(self.longitude)\(longLabel), \(self.radius)m))"
    }
    
    // calculates the distance in meters along the Earth's surface between the geofence and the given location
    func distanceFrom(latlong: CGPoint) -> Double {
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
}

func getRect(json: JSONDictionary) -> CGRect {
    return CGRect(origin: getPoint(json.getDictionary("origin")), size: getSize(json.getDictionary("size")))
}

func getPoint(json: JSONDictionary) -> CGPoint {
    return CGPoint(x: json.getDouble("x"), y: json.getDouble("y"))
}

func getSize(json: JSONDictionary) -> CGSize {
    return CGSize(width: json.getDouble("width"), height: json.getDouble("height"))
}