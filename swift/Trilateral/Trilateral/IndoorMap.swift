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

class IndoorMap: UIView, FlareController {
    
    var appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    var currentEnvironment: Environment? {
        didSet(value) {
            if value != currentEnvironment {
                for (_,label) in labels {
                    label.removeFromSuperview()
                }
                labels.removeAll(keepCapacity: true)
                
                if currentEnvironment != nil {
                    updateScale()
                    self.zones = currentEnvironment!.zones
                    self.things = currentEnvironment!.things()
                    setNeedsDisplay()
                }
            }
        }
    }
    var currentZone: Zone? { didSet(value) { /* highlight? */ }}
    var zones = [Zone]()
    var things = [Thing]()
    var device: Device? { didSet { setNeedsDisplay() }}
    var nearbyThing: Thing? { didSet { setNeedsDisplay() }}
    
    var labels = [String:UILabel]()
    
    var viewHeight: CGFloat = 768.0
    var gridCenter = CGPoint(x: 0,y: 0)
    var insetCenter = CGPoint(x: 0,y: 0)
    var gridOrigin = CGPoint(x: 0,y: 0)
    var scale: CGFloat = 1.0
    
    let lightGray = UIColor(red:0, green:0, blue:0, alpha:0.1)
    let pink = UIColor(red:1, green:0, blue:0, alpha:0.5)
    let blue = UIColor(red:0.4, green:0.4, blue:1, alpha:1.0)
    let lightBlue = UIColor(red:0, green:0, blue:1, alpha:0.15)
    let halo = UIColor(red:1, green:1, blue:0, alpha:0.5)
    let selectedColor = UIColor(red:48.0/256.0, green:131.0/256.0, blue:251.0/256.0, alpha:0.5)
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        NSNotificationCenter.defaultCenter().addObserver(self, selector:"orientationDidChange:", name: UIApplicationDidChangeStatusBarOrientationNotification, object: nil)
    }
    
    func orientationDidChange(note: NSNotification) {
        dataChanged()
    }

    func dataChanged() {
        self.setNeedsDisplay()
    }
    
    func animate() {
        self.setNeedsDisplay()
    }
    
    // sender.identifier can contain several words
    // the first word is the action
    @IBAction func performAction(sender: UIButton) {
        let identifiers = sender.accessibilityIdentifier!.componentsSeparatedByString(" ")
        let action = identifiers.first!
        
        if device != nil { appDelegate.flareManager.performAction(device!, action: action, sender: nil) }
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
    
    func viewWillTransitionToSize(size: CGSize, withTransitionCoordinator coordinator: UIViewControllerTransitionCoordinator) {
        NSLog("Will rotate")

        coordinator.animateAlongsideTransition(nil, completion: { context in
            NSLog("Completion")
            
            self.dataChanged()
        })
    }
            
    func updateScale() {
        let inset = CGRectInset(self.frame, 40, 40)
        let grid = currentEnvironment!.perimeter
        let xScale = inset.size.width / grid.size.width
        let yScale = inset.size.height / grid.size.height
        scale = (xScale < yScale) ? xScale : yScale
    }

    override func drawRect(rect: CGRect) {
        if (currentEnvironment != nil) {
            let context = UIGraphicsGetCurrentContext()
            CGContextScaleCTM(context, 1, -1);
            CGContextTranslateCTM(context, 0, -self.bounds.size.height);
            
            let inset = CGRectInset(self.frame, 40, 40)
            let grid = currentEnvironment!.perimeter
            
            updateScale()
            insetCenter = inset.center()
            gridCenter = grid.center()
            
            fillRect(grid, color: lightGray, inset: 0)
            
            for zone in zones {
                fillRect(zone.perimeter, color: lightGray, inset: 2)

                let label = labelForFlare(zone)
                label.center = flipPoint(convertPoint(zone.perimeter.center()))
            }

            if device != nil && nearbyThing != nil {
                let line = UIBezierPath()
                line.moveToPoint(convertPoint(device!.position))
                line.addLineToPoint(convertPoint(nearbyThing!.position))
                line.lineWidth = 3
                selectedColor.setStroke()
                line.stroke()
            }

            for thing in things {
                let color = IndoorMap.colorForThing(thing)
                
                if thing == nearbyThing { fillCircle(thing.position, radius: 15, color: selectedColor) }
                fillCircle(thing.position, radius: 10, color: color)
                
                let label = labelForFlare(thing)
                label.center = flipPoint(convertPoint(thing.position) + CGSize(width: 2, height: -22))
            }
            
            if device != nil && !device!.position.x.isNaN && !device!.position.y.isNaN {
                if nearbyThing != nil { fillCircle(device!.position, radius: 15, color: selectedColor) }
                fillCircle(device!.position, radius: 10, color: blue)
                
                let label = labelForFlare(device!)
                label.center = flipPoint(convertPoint(device!.position) + CGSize(width: 2, height: -22))
            }
        }
    }
    
    override func touchesEnded(touches: Set<UITouch>, withEvent event: UIEvent?) {
        if let touch = touches.first {
            let viewPoint = touch.locationInView(self)
            let gridPoint = undoConvertPoint(flipPoint(viewPoint))
            if let thing = thingNearPoint(gridPoint) {
                appDelegate.nearbyThing = thing
            }
        }
    }
    
    func thingNearPoint(point: CGPoint) -> Thing? {
        for thing in things {
            if thing.position - point < 1.0 {
                return thing
            }
        }
        return nil
    }
    
    static func colorForThing(thing: Thing) -> UIColor {
        var colorName = "red"
        var brightness = 0.5
        
        if let value = thing.data["color"] as? String {
            colorName = value
        }
        
        if let value = thing.data["brightness"] as? Double {
            brightness = value
        }
        
        return getColor(colorName, brightness: brightness)
    }

    static func getColor(name: String, brightness: Double) -> UIColor {
        if name == "clear" { return UIColor.clearColor() }
        if name == "white" { return UIColor(hue: 0, saturation: 0, brightness: 0.95, alpha: 1.0) }
        
        if let hex = LightManager.htmlColorNames[name] {
            return colorWithHex(hex)
        }
        
        return UIColor.redColor()
    }
    
    static func colorWithHex(rgbValue: Int) -> UIColor {
        return UIColor(red: CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0,
                     green: CGFloat((rgbValue & 0x00FF00) >>  8) / 255.0,
                      blue: CGFloat((rgbValue & 0x0000FF) >>  0) / 255.0,
                     alpha: 1.0)
    }
    
    static func hue(name: String) -> CGFloat {
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
    
    func undoConvertPoint(viewPoint: CGPoint) -> CGPoint {
        return CGPoint(x: gridCenter.x - (insetCenter.x - viewPoint.x) / scale,
            y: gridCenter.y - (insetCenter.y - viewPoint.y) / scale)
    }
}
