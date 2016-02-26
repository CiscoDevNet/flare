//
//  CompassView.swift
//  CompassTest
//
//  Created by Andrew Zamler-Carhart on 12/1/15.
//  Copyright © 2015 Cisco. All rights reserved.
//

import UIKit
import Flare

let inset: CGFloat = 20
let ring: CGFloat = 0.1

let minDistance: CGFloat = 1.0
let maxDistance: CGFloat = 12.0
let minSweep: CGFloat = 5
let maxSweep: CGFloat = 40

let backgroundColor = UIColor.grayColor()
let ringColor = UIColor(white: 0.2, alpha: 1.0)
let northColor = UIColor.redColor()

let showTicks = false
let tickColor = UIColor(white: 1.0, alpha: 0.3)
let tick2Color = UIColor(white: 1.0, alpha: 0.1)
let circleColor = UIColor.blackColor()

class CompassView: UIView, FlareController {
    
    let appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    var currentEnvironment: Environment? {
        didSet {
            updateFlipped()
            setNeedsDisplay()
        }
    }
    var flipped = false
    var currentZone: Zone? { didSet { setNeedsDisplay() }}
    var device: Device? { didSet { setNeedsDisplay() }}
    var selectedThing: Thing? { didSet { setNeedsDisplay() }}
    var nearbyThing: Thing? {
        didSet {
            updateLayout()
            updateThing()
            setNeedsDisplay()
        }
    }

    @IBOutlet weak var nearbyThingImage: UIImageView!
    @IBOutlet weak var nearbyThingLabel: UILabel!
    @IBOutlet weak var nearbyThingComment: UILabel!
    
    var offset: CGFloat = 0.0
    var heading: CGFloat = 0.0
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        appDelegate.flareController = self
        appDelegate.updateFlareController()
       
        NSNotificationCenter.defaultCenter().addObserver(self, selector:"orientationDidChange:", name: UIApplicationDidChangeStatusBarOrientationNotification, object: nil)
    }
    
    override func awakeFromNib() {
        super.awakeFromNib()
        updateLayout()
    }
    
    func orientationDidChange(note: NSNotification) {
        updateLayout()
        dataChanged()
    }
    
    func dataChanged() {
        if let imageName = nearbyThing?.imageName() {
            nearbyThingImage.image = UIImage(named: imageName)
        }

        setNeedsDisplay()
    }
    
    func animate() {
        self.setNeedsDisplay()
    }
    
    func updateLayout() {
        if nearbyThingLabel != nil && nearbyThingComment != nil {
            let y = (self.frame.size.height - nearbyThingImage.frame.size.height) / 2.0
            nearbyThingImage.frame.origin.y = y
            nearbyThingLabel.frame.origin.y = y + nearbyThingImage.frame.size.height - 5
            nearbyThingComment.frame.origin.y = y + nearbyThingImage.frame.size.height + 22
            nearbyThingImage.setNeedsDisplay()
            nearbyThingLabel.setNeedsDisplay()
            nearbyThingComment.setNeedsDisplay()
        }
    }
 
    func updateFlipped() {
        if currentEnvironment != nil {
            if let flipped = currentEnvironment!.data["flipped"] as? Int {
                self.flipped = flipped != 0
            } else {
                self.flipped = false
            }
        }
    }
    
    func updateThing() {
        if nearbyThingLabel != nil && nearbyThingComment != nil {
            nearbyThingLabel.text = nearbyThing?.name ?? ""
            nearbyThingComment.text = nearbyThing?.comment ?? ""
            
            if let imageName = nearbyThing?.imageName() {
                nearbyThingImage.image = UIImage(named: imageName)
            }
        }
    }
    
    override func drawRect(rect: CGRect) {
        // let background = NSBezierPath(rect: self.bounds)
        // backgroundColor.setFill()
        // background.fill()
        
        let bounds = self.bounds.insetBy(dx: inset, dy: inset)
        let center = bounds.center()
        
        let radiusX = bounds.size.width / 2
        let radiusY = bounds.size.height / 2
        let radius = min(radiusX, radiusY)
        
        let circleBounds = CGRect(x: center.x - radius, y: center.y - radius,
            width: 2 * radius, height: 2 * radius)
        
        let outerRing = UIBezierPath(ovalInRect: circleBounds)
        ringColor.setFill()
        outerRing.fill()
        
        let innerRing = UIBezierPath(ovalInRect: circleBounds.insetBy(dx: radius * ring, dy: radius * ring))
        circleColor.setFill()
        innerRing.fill()
        
        if currentEnvironment != nil && device != nil {
            offset = CGFloat(currentEnvironment!.angle)
            heading = CGFloat(device!.angle())
            
            for zone in currentEnvironment!.zones {
                if zone.perimeter.contains(device!.position) {
                    let things = zone.things.sort({device!.distanceTo($0) > device!.distanceTo($1)})
                    for thing in things {
                        let selected = selectedThing == thing || nearbyThing == thing
                        drawFin(thing, color: IndoorMap.colorForThing(thing), selected: selected,
                            center: center, radius: radius)
                    }
                }
            }
        }

        drawArc(tick2Color, center: center, radius: radius, direction: convertAngle(0.0), sweep: 360, thickness: 0.05)
        
        if showTicks {
            for i in 0...7 {
                let angle = 45 * CGFloat(i)
                let color = angle == 90.0 ? northColor : tickColor
                drawArc(color, center: center, radius: radius, direction: convertAngle(angle), sweep: 1, thickness: 0.1)
                drawArc(tick2Color, center: center, radius: radius, direction: convertAngle(22.5 + angle), sweep: 1, thickness: 0.1)
            }
        }
    }
    
    func convertAngle(angle: CGFloat) -> CGFloat {
        return (angle * (flipped ? -1 : 1) + heading - offset) % 360.0
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
    
    func drawFin(thing: Thing, color: UIColor, selected: Bool, center: CGPoint, radius: CGFloat) {
        if device != nil {
            let distance = CGFloat(device!.distanceTo(thing))
            if distance == 0 { return } // not meaningful to draw a fin if in the device and thing are in the same place
            let direction = CGFloat(device!.angleTo(thing))
            let sweep = sweepForDistance(distance)
            let thickness = CGFloat(selected ? 0.2 : 0.1)
            drawArc(color, center: center, radius: radius, direction: convertAngle(direction), sweep: sweep, thickness: thickness)
        }
    }
    
    func drawArc(color: UIColor, center: CGPoint, radius: CGFloat, direction: CGFloat, sweep: CGFloat, thickness: CGFloat) {
        let arc = UIBezierPath()
        let start: CGFloat = 0 - CGFloat(degreesToRadians(Double(direction - (sweep / 2.0))))
        let end: CGFloat = 0 - CGFloat(degreesToRadians(Double(direction + (sweep / 2.0))))
        arc.addArcWithCenter(center, radius: radius, startAngle: start, endAngle: end, clockwise: false)
        arc.addArcWithCenter(center, radius: radius * (1.0 - thickness), startAngle: end, endAngle: start, clockwise: true)
        arc.closePath()
        color.setFill()
        arc.fill()
    }

    // sender.identifier can contain several words
    // the first word is the action
    @IBAction func performAction(sender: UIButton) {
        let identifiers = sender.accessibilityIdentifier!.componentsSeparatedByString(" ")
        let action = identifiers.first!
        
        if device != nil { appDelegate.flareManager.performAction(device!, action: action, sender: nil) }
    }
    
    var touchedThing: Thing?
    
    override func touchesBegan(touches: Set<UITouch>, withEvent event: UIEvent?) {
        if let touch = touches.first {
            let point = touch.locationInView(self)
            let angle = atan2(point.y - self.center.y, point.x - self.center.x)
            var degrees = 0 - angle * CGFloat(180.0 / M_PI)
            if degrees < 0 { degrees += 360 }
            // NSLog("angle: \(degrees)")
            
            if let thing = thingAtAngle(degrees) {
                touchedThing = thing
            }
        }
    }
    
    override func touchesEnded(touches: Set<UITouch>, withEvent event: UIEvent?) {
        if touchedThing != nil {
            appDelegate.nearbyThing = touchedThing
        }
    }
    
    func thingAtAngle(angle: CGFloat) -> Thing? {
        if currentEnvironment != nil && device != nil {
            offset = CGFloat(currentEnvironment!.angle)
            heading = CGFloat(device!.angle())
            
            for zone in currentEnvironment!.zones {
                if zone.perimeter.contains(device!.position) {
                    let things = zone.things.sort({device!.distanceTo($0) > device!.distanceTo($1)})
                    for thing in things {
                        let distance = CGFloat(device!.distanceTo(thing))
                        if distance > 0 {
                            let direction = CGFloat(device!.angleTo(thing))
                            let sweep = sweepForDistance(distance)
                            var minAngle = convertAngle(direction - sweep / 2.0)
                            var maxAngle = convertAngle(direction + sweep / 2.0)
                            if minAngle < 0 { minAngle += 360 }
                            if maxAngle < 0 { maxAngle += 360 }
                            if minAngle > maxAngle { maxAngle += 360 }
                            // NSLog("\(thing.name) \(minAngle) \(maxAngle)")
                            if minAngle < angle && angle < maxAngle {
                                // NSLog("Found thing: \(thing.name)")
                                return thing
                            }
                        }

                    }
                }
            }
        }
        return nil
    }

}
