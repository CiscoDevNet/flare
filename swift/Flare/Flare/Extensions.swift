//
//  Extensions.swift
//  Facets Dashboard
//
//  Created by Andrew Zamler-Carhart on 16/09/2014.
//  Copyright (c) 2014 Cisco. All rights reserved.
//

import Foundation
import CoreGraphics

// a point translated by the given size
public func +(point: CGPoint, size: CGSize) -> CGPoint {
    let x = point.x + size.width
    let y = point.y + size.height
    return CGPoint(x:x, y:y)
}

// a point negatively translated by the given size
public func -(point: CGPoint, size: CGSize) -> CGPoint {
    let x = point.x - size.width
    let y = point.y - size.height
    return CGPoint(x:x , y:y)
}

// the distance between two points (width and height)
/*
func -(point1: CGPoint, point2: CGPoint) -> CGSize {
    let width = point1.x - point2.x
    let height = point1.y - point2.y
    return CGSize(width:width, height:height)
}
*/

// the distance between two points (diagonal)
public func -(point1: CGPoint, point2: CGPoint) -> Double {
    let dx = point1.x - point2.x
    let dy = point1.y - point2.y
    return Double(sqrt(dx * dx + dy * dy))
}

// the difference between two sizes
public func -(size1: CGSize, size2: CGSize) -> CGSize {
    let width = size1.width - size2.width
    let height = size1.height - size2.height
    return CGSize(width:width, height:height)
}

// a point scaled by the given ratio
public func *(point: CGPoint, ratio: CGFloat) -> CGPoint {
    let x = point.x * ratio
    let y = point.y * ratio
    return CGPoint(x:x, y:y)
}

// a size scaled by the given ratio
public func *(size: CGSize, ratio: CGFloat) -> CGSize {
    let width = size.width * ratio
    let height = size.height * ratio
    return CGSize(width:width, height:height)
}

// a rect scaled by the given ratio
public func *(rect: CGRect, ratio: CGFloat) -> CGRect {
    let origin = rect.origin * ratio
    let size = rect.size * ratio
    return CGRect(origin:origin, size:size)
}

// returns a random Int between 0 and limit - 1
public func randomInt(limit:Int) -> Int {
    return limit > 0 ? Int(arc4random_uniform(UInt32(limit))) : 0
}

// returns a random Int between min and max inclusive
public func randomInt(min: Int, max: Int) -> Int {
    return min + randomInt(max - min + 1)
}

// a random size with each dimension between min and max
public func randomSize(min: Int, max: Int) -> CGSize {
    let width = randomInt(min, max)
    let height = randomInt(min, max)
    return CGSize(width:width, height:height)
}

extension Array {
    
    // returns a random object from the array
    func randomObject() -> T  {
        return self[randomInt(self.count)]
    }

    // removes an object from the array by value
    mutating func removeObject<U: Equatable>(object: U) {
        var index: Int?
        for (idx, objectToCompare) in enumerate(self) {
            if let to = objectToCompare as? U {
                if object == to {
                    index = idx
                }
            }
        }
        
        if (index != nil) {
            self.removeAtIndex(index!)
        }
    }
}

public extension Double {
    
    // returns the number rounded to the given precision
    // for example, 17.37.roundTo(0.5) returns 17.5
    public func roundTo(precision: Double) -> Double {
        return (self + precision / 2.0).roundDown(precision)
    }
    
    // returns the number rounded down to the given precision
    // for example, 17.37.roundTo(0.5) returns 17.0
    public func roundDown(precision: Double) -> Double {
        return Double(Int(self / precision)) * precision
    }
}

public extension CGSize {
    
    // returns the diagonal length
    public func length() -> Double {
        let x2 = Double(self.width * self.width)
        let y2 = Double(self.height * self.height)
        return sqrt(x2 + y2)
    }
}

public extension CGPoint {
    
    // rounds the point's x and y values to the given precision
    public func roundTo(precision: Double) -> CGPoint {
        return CGPoint(x: self.x.roundTo(precision), y: self.y.roundTo(precision))
    }
}

public extension CGFloat {
    
    // adds the same rounding behavior to CGFloat
    public func roundTo(precision: Double) -> Double {
        return Double(self).roundTo(precision)
    }
}

public extension Int {
    
    // calls the closure a number of times
    // e.g. 5.times { // do stuff }
    public func times(closure: () -> ()) {
        if self > 0 {
            for index in 1...self {
                closure()
            }
        }
    }
}

// calls the closure in the main queue
public func queue(closure:()->()) {
    dispatch_async(dispatch_get_main_queue(), closure)
}

// calls the closure in the background
public func background(closure:()->()) {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), closure)
}

// calls the closure after the delay
// e.g. delay(5.0) { // do stuff }
public func delay(delay:Double, closure:()->()) {
    dispatch_after(
        dispatch_time(
            DISPATCH_TIME_NOW,
            Int64(delay * Double(NSEC_PER_SEC))
        ),
        dispatch_get_main_queue(), closure)
}

public extension NSMutableString {
    
    // replaces all occurrences of one string with another
    public func replace(target: String, with: String) {
        self.replaceOccurrencesOfString(target, withString: with, options: nil, range: NSRange(location: 0, length: self.length))
    }
}

func radiansToDegrees(radians: Double) -> Double {
    return radians * 180.0 / M_PI;
}

