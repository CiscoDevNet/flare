//
//  FlareManager.swift
//  Trilateral
//
//  Created by Andrew Zamler-Carhart on 3/25/15.
//  Copyright (c) 2015 Cisco. All rights reserved.
//

import Foundation
import CoreLocation
import CoreGraphics

public protocol BeaconManagerDelegate {
    func devicePositionDidChange(position: CGPoint)
    func deviceLocationDidChange(location: CLLocation)
}

public class BeaconManager: NSObject, CLLocationManagerDelegate {
    
    let squareDistance = false

    public var delegate: BeaconManagerDelegate?
    public var locationManager = CLLocationManager()
    public var region: CLBeaconRegion?

    public var currentLatlong: CLLocation?
    
    public var environment: Environment?
    public var beacons = [Int:Thing]()

    public override init() {
        super.init()
        
        self.locationManager.delegate = self
        // self.locationManager.requestWhenInUseAuthorization()
        self.locationManager.requestAlwaysAuthorization()
    }

    public func loadEnvironment(value: Environment) {
        self.environment = value;
        
        if (environment != nil) {
            if let uuidString = environment!.uuid {
                let uuid = NSUUID(UUIDString: uuidString)
                region = CLBeaconRegion(proximityUUID: uuid!, identifier: environment!.name)
                beacons = environment!.beacons()
                // NSLog("Looking for \(beacons.count) beacons.")
            }
        }
    }
    
    public func start() {
        if region != nil {
            self.locationManager.startRangingBeaconsInRegion(region!)
        }
    }

    public func stop() {
        if region != nil {
            self.locationManager.stopRangingBeaconsInRegion(region!)
        }
    }

    public func startMonitoringLocation() {
        self.locationManager.startMonitoringSignificantLocationChanges()
    }
    
    public func stopMonitoringLocation() {
        self.locationManager.stopMonitoringSignificantLocationChanges()
    }
    
    public func locationManager(manager: CLLocationManager, didChangeAuthorizationStatus status: CLAuthorizationStatus) {
        switch status {
            case .NotDetermined: NSLog("Not determined")
            case .Restricted: NSLog("Restricted")
            case .Denied: NSLog("Denied")
            case .AuthorizedAlways: NSLog("Authorized Always")
            case .AuthorizedWhenInUse: NSLog("Authorized When In Use")
        }
    }

    public func locationManager(manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        NSLog("Did update locations")
        if let location = locations.last {
            // NSLog("Location: \(location)")
            currentLatlong = location
            
            if delegate != nil {
                delegate!.deviceLocationDidChange(location)
            }
        }
    }

    public func locationManager(manager: CLLocationManager, didRangeBeacons clbeacons: [CLBeacon], inRegion region: CLBeaconRegion) {
        var clBeaconIndex = [Int:CLBeacon]()
        
        // NSLog("Found \(clbeacons.count) beacons.")
        
        for clbeacon in clbeacons {
            // NSLog("Found beacon with minor: \(clbeacon.minor.integerValue)")
            clBeaconIndex[clbeacon.minor.integerValue] = clbeacon
        }
        
        for (_,beacon) in beacons {
            if let clbeacon = clBeaconIndex[beacon.minor!] {
                // NSLog("Found beacon: \(beacon.name)")
                beacon.addDistance(clbeacon.accuracy)
            } else {
                // NSLog("Couldn't find beacon: \(beacon.name)")
                beacon.addDistance(-1.0) // the beacon was not seen this time
            }
        }
        
        if delegate != nil {
            delegate!.devicePositionDidChange(weightedLocation(false))
        }
    }
    
    // the average position of all nearby beacons,
    // weighted according to the inverse of the square of the distance
    public func weightedLocation(average: Bool) -> CGPoint {
        var total = 0.0
        var x = 0.0
        var y = 0.0
        
        for (_,beacon) in beacons {
            let distance = average ? beacon.averageDistance() : beacon.lastDistance()
            if distance > 0 {
                beacon.inverseDistance = 1.0 / (distance * distance)
            } else {
                beacon.inverseDistance = -1
            }
        }

        var sortedBeacons = [Thing](beacons.values)
        sortedBeacons.sortInPlace { (one: Thing, two: Thing) -> Bool in
            return one.inverseDistance > two.inverseDistance
        }
        
        for (_,beacon) in sortedBeacons.enumerate() {
            if beacon.inverseDistance != -1 {
                let weight = beacon.inverseDistance /* * (index < 2 ? 2 : 1) */
                x += Double(beacon.position.x) * weight
                y += Double(beacon.position.y) * weight
                total += weight
            }
        }
        
        let result = CGPoint(x:x / total, y:y / total)
        // NSLog("Result: \(result)")
        return result
    }
}

/*

func userPosition(dA: CGFloat, dB: CGFloat, dC: CGFloat) -> CGPoint {
    // var beacon1 = CGPoint(x: 2.5, y: -0.5)
    // var beacon2 = CGPoint(x: 11, y: 11.5)
    // var beacon3 = CGPoint(x: 3, y: 11.5)
    
    var a = CGPoint(x: 0, y: 0)
    var b = CGPoint(x: 10, y: 10)
    var c = CGPoint(x: 0, y: 10)
    
    /*
    var xa = beacon1.x
    var ya = beacon1.y
    var xb = beacon2.x
    var yb = beacon2.y
    var xc = beacon3.x
    var yc = beacon3.y
    
    var ra = distance1
    var rb = distance2
    var rc = distance3
    
    var S2 = xc.sq() - xb.sq() + yc.sq() - yb.sq() + rb.sq() - rc.sq()
    var S = S2 / 2.0
    var T2 = xa.sq() - xb.sq() + ya.sq() - yb.sq() + rb.sq() - ra.sq()
    var T = T2 / 2.0
    var ytop = (T * (xb - xc)) - (S * (xb - xa))
    var ybottom = ((ya - yb) * (xb - xc)) - ((yc - yb) * (xb - xa))
    var y = ytop / ybottom
    var x = ((y * (ya - yb)) - T) / (xb - xa)
    */
    
    /*
    var W = dA*dA - dB*dB - a.x*a.x - a.y*a.y + b.x*b.x + b.y*b.y
    var Z = dB*dB - dC*dC - b.x*b.x - b.y*b.y + c.x*c.x + c.y*c.y
    
    var xtop = W*(c.y-b.y) - Z*(b.y-a.y)
    var xbottom = ((b.x-a.x)*(c.y-b.y) - (c.x-b.x)*(b.y-a.y))
    var x = xtop / (2 * xbottom)
    var y = (W - 2*x*(b.x-a.x)) / (2*(b.y-a.y))
    var y2 = (Z - 2*x*(c.x-b.x)) / (2*(c.y-b.y))
    
    // y = (y + y2) / 2;
    */

    return CGPoint(x: x, y: y)
}


extension Double {
    func sq() -> Double {
        return self * self
    }
}

extension CGFloat {
    func sq() -> CGFloat {
        return self * self
    }
}

*/