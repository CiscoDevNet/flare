//
//  IndoorMap.swift
//  Trilateral
//
//  Created by Andrew Zamler-Carhart on 3/26/15.
//  Copyright (c) 2015 Cisco. All rights reserved.
//

import UIKit
import Flare
import CoreGraphics

class IndoorMap: UIView {
    
    var environment: Environment?
    var zones = [Zone]()
    var beacons = [Int:Thing]()
    var device: Device?
    var nearbyThing: Thing? {
        didSet {
            setNeedsDisplay()
        }
    }
    
    var labels = [String:UILabel]()
    
    var viewHeight: CGFloat = 768.0
    var gridCenter = CGPoint(x: 0,y: 0)
    var insetCenter = CGPoint(x: 0,y: 0)
    var gridOrigin = CGPoint(x: 0,y: 0)
    var scale: CGFloat = 1.0
    
    let lightGray = UIColor(red:0, green:0, blue:0, alpha:0.1)
    let pink = UIColor(red:1, green:0, blue:0, alpha:0.5)
    let blue = UIColor(red:0.4, green:0.4, blue:1, alpha:1.0)
    let halo = UIColor(red:1, green:1, blue:0, alpha:0.5)
    
    func loadEnvironment(value: Environment) {
        for (_,label) in labels {
            label.removeFromSuperview()
        }
        labels.removeAll(keepCapacity: true)

        self.environment = value
        
        if environment != nil {
            self.zones = environment!.zones
            self.beacons = environment!.beacons()
            self.setNeedsDisplay()
        }
    }
    
    func dataChanged() {
        self.setNeedsDisplay()
    }
    
    func updateLabels() {
        for (_,beacon) in beacons {
            if let label = labels[beacon.id] {
                label.text = String(format:"%.2f", Float(beacon.lastDistance()))
            }
        }
    }
    
    func labelForFlare(flare: Flare) -> UILabel {
        var label = labels[flare.id]
        if (label == nil) {
            label = UILabel(frame: CGRectMake(0, 0, 200, 21))
            label!.textAlignment = NSTextAlignment.Center
            label!.textColor = UIColor.grayColor()
            label!.text = flare.name
            self.addSubview(label!)
            labels[flare.id] = label!
        }
        return label!
    }
    
    override func drawRect(rect: CGRect) {
        if (environment != nil) {
            let context = UIGraphicsGetCurrentContext()
            CGContextScaleCTM(context, 1, -1);
            CGContextTranslateCTM(context, 0, -self.bounds.size.height);
            
            let inset = CGRectInset(rect, 40, 40)
            let grid = environment!.perimeter
            
            insetCenter = centerPoint(inset)
            gridCenter = centerPoint(grid)
            
            let xScale = inset.size.width / grid.size.width
            let yScale = inset.size.height / grid.size.height
            scale = (xScale < yScale) ? xScale : yScale
            
            fillRect(grid, color: lightGray, inset: 0)
            
            for zone in zones {
                fillRect(zone.perimeter, color: lightGray, inset: 2)

                let label = labelForFlare(zone)
                label.center = flipPoint(convertPoint(centerPoint(zone.perimeter)))
            }

            if device != nil && nearbyThing != nil {
                let line = UIBezierPath()
                line.moveToPoint(convertPoint(device!.position))
                line.addLineToPoint(convertPoint(nearbyThing!.position))
                line.lineWidth = 3
                halo.setStroke()
                line.stroke()
            }

            for (_,beacon) in beacons {
                var colorName = "red"
                var brightness = 0.5
                    
                if let value = beacon.data["color"] as? String {
                    colorName = value
                }
                
                if let value = beacon.data["brightness"] as? Double {
                    brightness = value
                }
                
                let color = getColor(colorName, brightness: brightness)
                
                if beacon == nearbyThing { fillCircle(beacon.position, radius: 15, color: halo) }
                fillCircle(beacon.position, radius: 10, color: color)
                
                let label = labelForFlare(beacon)
                label.center = flipPoint(convertPoint(beacon.position) + CGSize(width: 2, height: -22))
            }
            
            if device != nil {
                if nearbyThing != nil { fillCircle(device!.position, radius: 15, color: halo) }
                fillCircle(device!.position, radius: 10, color: blue)
                
                let label = labelForFlare(device!)
                label.center = flipPoint(convertPoint(device!.position) + CGSize(width: 2, height: -22))
            }
        }
    }
    
    func getColor(name: String, brightness: Double) -> UIColor {
        return UIColor(hue: hue(name),
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
    
    func fillRect(rect: CGRect, color: UIColor, inset: CGFloat) {
        let path = UIBezierPath(rect: CGRectInset(convertRect(rect), inset, inset))
        color.setFill()
        path.fill()
    }
    
    func fillCircle(center: CGPoint, radius: CGFloat, color: UIColor) {
        let newCenter = convertPoint(center)
        let rect = CGRect(x: newCenter.x - radius, y: newCenter.y - radius, width: radius * 2, height: radius * 2)
        let path = UIBezierPath(ovalInRect: rect)
        color.setFill()
        path.fill()
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
