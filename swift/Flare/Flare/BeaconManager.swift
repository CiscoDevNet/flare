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
    func deviceAngleDidChange(angle: Double)
}

public class BeaconManager: NSObject, CLLocationManagerDelegate {
    
    let squareDistance = false
    let beaconDebug = false
    
    public var delegate: BeaconManagerDelegate?
    public var locationManager = CLLocationManager()
    public var region: CLBeaconRegion?

    public var currentLatlong: CLLocation?
    
    public var environment: Environment?
    public var beacons = [Int:Thing]()
    public var linearBeacons = [Thing]()
    
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
                linearBeacons = [Thing](beacons.values)
                linearBeacons.sortInPlace({ $0.minor > $1.minor })
                if beaconDebug { NSLog("Looking for \(beacons.count) beacons.") }
            } else {
                NSLog("Environment has no uuid.")
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
    
    public func startUpdatingHeading() {
        self.locationManager.startUpdatingHeading()
    }
    
    public func stopUpdatingHeading() {
        self.locationManager.stopUpdatingHeading()
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
        // NSLog("Did update locations")
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
        
        if beaconDebug { NSLog("Found \(clbeacons.count) beacons.") }

        for clbeacon in clbeacons {
            let index = clbeacon.major.integerValue * 10000 + clbeacon.minor.integerValue
            if beaconDebug { NSLog("Saw beacon: \(clbeacon.major.integerValue) - \(clbeacon.minor.integerValue)") }
            clBeaconIndex[index] = clbeacon
        }
        
        for (index, beacon) in beacons {
            if let clbeacon = clBeaconIndex[index] {
                if beaconDebug { NSLog("Found beacon: \(beacon.name)") }
                beacon.addDistance(clbeacon.accuracy)
            } else {
                if beaconDebug { NSLog("Couldn't find beacon: \(beacon.name) (\(index))") }
                beacon.addDistance(-1.0) // the beacon was not seen this time
            }
        }
        
        if delegate != nil {
            let position = weightedLocation(false)
            if !position.x.isNaN && !position.y.isNaN {
                delegate!.devicePositionDidChange(position.roundTo(0.01))
            }
        }
    }
    
    var angleDelay = 1.0
    var lastAngleTime = NSDate()
    var lastAngle = -1.0

    public func locationManager(manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        let newAngle = newHeading.magneticHeading.roundTo(5.0)
        
        if newAngle != lastAngle && lastAngleTime.timeIntervalSinceNow < -angleDelay {
            lastAngleTime = NSDate()
            lastAngle = newAngle
            delegate!.deviceAngleDidChange(newAngle)
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
        sortedBeacons.sortInPlace { $0.inverseDistance > $1.inverseDistance }
        
        // let trainDemo = true
        // var nearest: Thing?
        // var secondNearest: Thing?
        
        for (_,beacon) in sortedBeacons.enumerate() {
            if beacon.inverseDistance != -1 {

                // for tracking position on a linear circuit,
                // only use the nearest beacon and the one before or after it
                /* if trainDemo {
                    if nearest == nil {
                        nearest = beacon
                        NSLog("Nearest: \(nearest!.name)")
                    } else if secondNearest == nil {
                        if beacon.minor == nearest!.minor! + 1 || beacon.minor! == nearest!.minor! - 1 {
                            secondNearest = beacon
                            NSLog("Second nearest: \(secondNearest!.name)")
                        } else {
                            // other nearby beacon is not consecutive with the nearest one, so ignore it
                            continue
                        }
                    } else {
                        // we have already found two beacons, ignore the rest
                        continue
                    }
                } */
                
                let weight = beacon.inverseDistance
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
