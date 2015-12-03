//
//  CompassView.swift
//  CompassTest
//
//  Created by Andrew Zamler-Carhart on 12/1/15.
//  Copyright © 2015 Cisco. All rights reserved.
//

import Cocoa
import Flare

let inset: CGFloat = 20
let ring: CGFloat = 0.1

let minDistance: CGFloat = 1.0
let maxDistance: CGFloat = 12.0
let minSweep: CGFloat = 5
let maxSweep: CGFloat = 40

let backgroundColor = NSColor.grayColor()
let ringColor = NSColor(calibratedWhite: 0.2, alpha: 1.0)
let tickColor = NSColor(calibratedWhite: 1.0, alpha: 0.3)
let tick2Color = NSColor(calibratedWhite: 1.0, alpha: 0.1)
let circleColor = NSColor.blackColor()

class CompassView: NSView {
    
    var environment: Environment?
    var device: Device?
    var selectedThing: Thing?
    
    func dataChanged() {
        self.needsDisplay = true
    }
    
    override func drawRect(rect: CGRect) {
        // let background = NSBezierPath(rect: self.bounds)
        // backgroundColor.setFill()
        // background.fill()
        
        let bounds = self.bounds.insetBy(dx: inset, dy: inset)
        let center = centerPoint(bounds)
        
        let radiusX = bounds.size.width / 2
        let radiusY = bounds.size.height / 2
        let radius = min(radiusX, radiusY)
        
        let circleBounds = CGRect(x: center.x - radius, y: center.y - radius,
            width: 2 * radius, height: 2 * radius)
        
        let outerRing = NSBezierPath(ovalInRect: circleBounds)
        ringColor.setFill()
        outerRing.fill()
        
        let innerRing = NSBezierPath(ovalInRect: circleBounds.insetBy(dx: radius * ring, dy: radius * ring))
        circleColor.setFill()
        innerRing.fill()
        
        // drawFin(CGPoint(x: 5, y: 12), color: NSColor.redColor(), selected: false, center: center, radius: radius)
        // drawFin(CGPoint(x: 7, y: 7), color: NSColor.greenColor(), selected: false, center: center, radius: radius)
        // drawFin(CGPoint(x: 7, y: 5), color: NSColor.blueColor(), selected: true, center: center, radius: radius)
        
        if environment != nil && device != nil {
            for zone in environment!.zones {
                if zone.perimeter.contains(device!.position) {
                    for thing in zone.things {
                        drawFin(thing, color: IndoorMap.colorForThing(thing), selected: selectedThing == thing,
                            center: center, radius: radius)
                    }
                }
            }
        }
        
        drawArc(tick2Color, center: center, radius: radius, direction: 0, sweep: 360, thickness: 0.05)
        for i in 0...7 {
            let angle = 45 * CGFloat(i)
            drawArc(tickColor, center: center, radius: radius, direction: angle, sweep: 1, thickness: 0.1)
            drawArc(tick2Color, center: center, radius: radius, direction: 22.5 + angle, sweep: 1, thickness: 0.1)
        }
    }

    func angleTowards(one: CGPoint, two: CGPoint) -> CGFloat {
        return CGFloat(90) - CGFloat(radiansToDegrees(atan2(Double(two.x - one.x), Double(two.y - one.y))))
    }
    
    func sweepForDistance(distance: CGFloat) -> CGFloat {
        if distance < minDistance {
            return maxSweep
        } else if distance > maxDistance {
            return minSweep
        } else {
            return (minSweep + (maxDistance - distance) / (maxDistance - minDistance) * (maxSweep - minSweep))
        }
    }
    
    func drawFin(thing: Thing, color: NSColor, selected: Bool, center: CGPoint, radius: CGFloat) {
        if device != nil {
            let distance = CGFloat(device!.distanceTo(thing))
            if distance == 0 { return } // not meaningful to draw a fin if in the device and thing are in the same place
            let direction = CGFloat(device!.angleTo(thing))
            let sweep = sweepForDistance(distance)
            let thickness = CGFloat(selected ? 0.2 : 0.1)
            drawArc(color, center: center, radius: radius, direction: direction, sweep: sweep, thickness: thickness)
        }
    }
    
    func drawArc(color: NSColor, center: CGPoint, radius: CGFloat, direction: CGFloat, sweep: CGFloat, thickness: CGFloat) {
        let arc = NSBezierPath()
        let start: CGFloat = direction - (sweep / 2.0)
        let end: CGFloat = direction + (sweep / 2.0)
        arc.appendBezierPathWithArcWithCenter(center, radius: radius,
            startAngle: start, endAngle: end, clockwise: false)
        arc.appendBezierPathWithArcWithCenter(center, radius: radius * (1.0 - thickness),
            startAngle: end, endAngle: start, clockwise: true)
        arc.closePath()
        color.setFill()
        arc.fill()
    }
    
    func centerPoint(rect: CGRect) -> CGPoint {
        return CGPoint(x: CGRectGetMidX(rect), y: CGRectGetMidY(rect))
    }
    
}
