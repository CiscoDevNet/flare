//
//  IndoorMap.swift
//  Explorer
//
//  Created by Andrew Zamler-Carhart on 8/17/15.
//  Copyright (c) 2015 Andrew Zamler-Carhart. All rights reserved.
//

import Cocoa
import Flare
import CoreGraphics

@IBDesignable
class IndoorMap: NSView {
    
    var environment: Environment?
    var zones = [Zone]()
    var things = [Thing]()
    var nearbyThing: Thing? {
        didSet {
            self.needsDisplay = true
        }
    }
    
    var labels = [String:NSTextField]()
    
    var viewHeight: CGFloat = 768.0
    var gridCenter = CGPoint(x: 0,y: 0)
    var insetCenter = CGPoint(x: 0,y: 0)
    var gridOrigin = CGPoint(x: 0,y: 0)
    var scale: CGFloat = 1.0
    
    let white = NSColor(red:255, green:255, blue:255, alpha:0.5)
    let lightGray = NSColor(red:0, green:0, blue:0, alpha:0.1)
    let pink = NSColor(red:255, green:0, blue:0, alpha:0.5)
    let blue = NSColor(red:0, green:0, blue:255, alpha:0.5)
    let lightBlue = NSColor(red:0, green:0, blue:255, alpha:0.15)
    let halo = NSColor(red:255, green:255, blue:0, alpha:0.5)
    
    func loadEnvironment(value: Environment) {
        if value != environment {
            for (_,label) in labels {
                label.removeFromSuperview()
            }
            labels.removeAll(keepCapacity: true)
            
            self.environment = value
            
            if environment != nil {
                updateScale()
                self.zones = environment!.zones
                self.things = environment!.things()
                self.needsDisplay = true
            }
        }
    }
    
    func dataChanged() {
        self.needsDisplay = true
    }
    
    func labelForFlare(flare: Flare) -> NSTextField {
        var label = labels[flare.id]
        if (label == nil) {
            label = NSTextField(frame: CGRectMake(0, 0, 200, 21))
            label!.font = NSFont.systemFontOfSize(13)
            label!.textColor = NSColor.blackColor()
            label!.stringValue = flare.name
            label!.editable = false
            label!.drawsBackground = false
            label!.bezeled = false
            label!.alignment = NSTextAlignment.Center
            self.addSubview(label!)
            labels[flare.id] = label!
        }
        return label!
    }

    func updateScale() {
        let inset = CGRectInset(self.frame, 20, 20)
        let grid = environment!.perimeter
        let xScale = inset.size.width / grid.size.width
        let yScale = inset.size.height / grid.size.height
        scale = (xScale < yScale) ? xScale : yScale
    }
    
    override func drawRect(rect: CGRect) {
        if (environment != nil) {
            let inset = CGRectInset(NSRect(origin: CGPointZero, size: self.frame.size), 20, 20)
            let grid = environment!.perimeter

            updateScale()
            insetCenter = centerPoint(inset)
            gridCenter = centerPoint(grid)
            
            fillRect(grid, color: white, inset: 0)
            
            for zone in zones {
                fillRect(zone.perimeter, color: white, inset: 2)
                
                let label = labelForFlare(zone)
                label.frame.origin = convertPoint(centerPoint(zone.perimeter)) - CGSize(width: 100, height: 10)
            }
            
            for thing in things {
                var colorName = "red"
                var brightness = 0.5
                
                if let value = thing.data["color"] as? String {
                    colorName = value
                }
                
                if let value = thing.data["brightness"] as? Double {
                    brightness = value
                }
                
                let color = getColor(colorName, brightness: brightness)
                
                if thing == nearbyThing {
                    fillCircle(thing.position, radius: 15, color: halo)
                }
                fillCircle(thing.position, radius: 10, color: color)
                
                let label = labelForFlare(thing)
                label.frame.origin = convertPoint(thing.position) - CGSize(width: 100, height: 33)
            }

            for device in environment!.devices {
                fillCircle(device.position, radius: 10, color: blue)

                let label = labelForFlare(device)
                label.frame.origin = convertPoint(device.position) - CGSize(width: 100, height: 33)
                /*
                // example of using distanceTo() and angleTo()
                for thing in things {
                    let distance = device.distanceTo(thing)
                    let angle = device.angleTo(thing)
                    NSLog("\(device.name) to \(thing.name): \(distance) meters, \(angle) degrees")
                }
                */
            }
        }
    }
    
    func getColor(name: String, brightness: Double) -> NSColor {
        return NSColor(hue: hue(name),
            saturation: CGFloat(1.0),
            brightness: CGFloat(brightness * 2.0),
            alpha: CGFloat(1.0))
    }
    
    func hue(name: String) -> CGFloat {
        if name == "red" { return 0 }
        if name == "orange" { return 0.08333333 }
        if name == "yellow" { return 0.16666666 }
        if name == "green" { return 0.3333333 }
        if name == "blue" { return 0.66666666 }
        if name == "purple" { return 0.7777777 }
        return 0
    }
    
    func fillRect(rect: CGRect, color: NSColor, inset: CGFloat) {
        let converted = convertRect(rect)
        let inset = CGRectInset(converted, inset, inset)
        if inset.size.width > 0 && inset.size.height > 0 {
            let path = NSBezierPath(rect: inset)
            color.setFill()
            path.fill()
        }
    }
    
    func fillCircle(center: CGPoint, radius: CGFloat, color: NSColor) {
        let newCenter = convertPoint(center)
        let rect = CGRect(x: newCenter.x - radius, y: newCenter.y - radius, width: radius * 2, height: radius * 2)
        if rect.origin.x < 10000000 && rect.origin.y < 10000000 {
            let path = NSBezierPath(ovalInRect: rect)
            color.setFill()
            path.fill()
        }
    }
    
    func centerPoint(rect: CGRect) -> CGPoint {
        return CGPoint(x: CGRectGetMidX(rect), y: CGRectGetMidY(rect))
    }
    
    func flipPoint(point: CGPoint) -> CGPoint {
        return CGPoint(x: point.x, y: self.bounds.height - point.y)
    }
    
    func convertPoint(gridPoint: CGPoint) -> CGPoint {
        return CGPoint(x: round(insetCenter.x - (gridCenter.x - gridPoint.x) * scale),
            y: round(insetCenter.y - (gridCenter.y - gridPoint.y) * scale))
    }
    
    func convertSize(gridSize: CGSize) -> CGSize {
        return CGSize(width: round(gridSize.width * scale), height: round(gridSize.height * scale))
    }
    
    func convertRect(gridRect: CGRect) -> CGRect {
        return CGRect(origin: convertPoint(gridRect.origin), size: convertSize(gridRect.size))
    }
}
