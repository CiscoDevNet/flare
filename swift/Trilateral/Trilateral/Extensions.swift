//
//  Extensions.swift
//  Facets Dashboard
//
//  Created by Andrew Zamler-Carhart on 16/09/2014.
//  Copyright (c) 2014 Cisco. All rights reserved.
//

import Foundation
import CoreGraphics

// converts NSData to an NSDictionary
extension NSData {
    func toJSONDictionary() -> JSONDictionary? {
        let json: JSONDictionary?
        do {
            json = try NSJSONSerialization.JSONObjectWithData(self, options: []) as? JSONDictionary
        } catch _ {
            json = nil
        }
        return json
    }
}

// converts a JSONDictionary to NSData
extension Dictionary {
    func toData() -> NSData? {
        let data: NSData?
        do {
            data = try NSJSONSerialization.dataWithJSONObject(self as! AnyObject, options:[])
        } catch _ {
            data = nil
        }
        return data
    }
}

// a point translated by the given size
func +(point: CGPoint, size: CGSize) -> CGPoint {
    let x = point.x + size.width
    let y = point.y + size.height
    return CGPoint(x:x, y:y)
}

// a point negatively translated by the given size
func -(point: CGPoint, size: CGSize) -> CGPoint {
    let x = point.x - size.width
    let y = point.y - size.height
    return CGPoint(x:x , y:y)
}

// the distance between two points
func -(point1: CGPoint, point2: CGPoint) -> CGFloat {
    let width = point1.x - point2.x
    let height = point1.y - point2.y
    return sqrt(width * width + height * height)
}

// the difference between two sizes
func -(size1: CGSize, size2: CGSize) -> CGSize {
    let width = size1.width - size2.width
    let height = size1.height - size2.height
    return CGSize(width:width, height:height)
}

// a point scaled by the given ratio
func *(point: CGPoint, ratio: CGFloat) -> CGPoint {
    let x = point.x * ratio
    let y = point.y * ratio
    return CGPoint(x:x, y:y)
}

// a size scaled by the given ratio
func *(size: CGSize, ratio: CGFloat) -> CGSize {
    let width = size.width * ratio
    let height = size.height * ratio
    return CGSize(width:width, height:height)
}

// a rect scaled by the given ratio
func *(rect: CGRect, ratio: CGFloat) -> CGRect {
    let origin = rect.origin * ratio
    let size = rect.size * ratio
    return CGRect(origin:origin, size:size)
}

// returns a random Int between 0 and limit - 1
func randomInt(limit:Int) -> Int {
    return limit > 0 ? Int(arc4random_uniform(UInt32(limit))) : 0
}

// returns a random Int between min and max inclusive
func randomInt(min: Int, max: Int) -> Int {
    return min + randomInt(max - min + 1)
}

// a random size with each dimension between min and max
func randomSize(min: Int, max: Int) -> CGSize {
    let width = randomInt(min, max: max)
    let height = randomInt(min, max: max)
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
        for (idx, objectToCompare) in self.enumerate() {
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

extension Double {
    
    // returns the number rounded to the given precision
    // for example, 17.37.roundTo(0.5) returns 17.5
    func roundTo(precision: Double) -> Double {
        return (self + precision / 2.0).roundDown(precision)
    }
    
    // returns the number rounded down to the given precision
    // for example, 17.37.roundTo(0.5) returns 17.0
    func roundDown(precision: Double) -> Double {
        if self.isNaN { return self }
        return Double(Int(self / precision)) * precision
    }
}

extension CGRect {
    
    // returns the point at the center of the rectangle
    func center() -> CGPoint {
        return CGPoint(x: (self.minX + self.maxX) / 2.0,
                       y: (self.minY + self.maxY) / 2.0)
    }
}

extension CGSize {
    
    // returns the diagonal length
    func length() -> Double {
        let x2 = Double(self.width * self.width)
        let y2 = Double(self.height * self.height)
        return sqrt(x2 + y2)
    }
}

extension CGPoint {
    
    // rounds the point's x and y values to the given precision
    func roundTo(precision: Double) -> CGPoint {
        return CGPoint(x: self.x.roundTo(precision), y: self.y.roundTo(precision))
    }
    
    func toJSON() -> JSONDictionary {
        return ["x":self.x, "y":self.y]
    }
}

extension CGFloat {
    
    // adds the same rounding behavior to CGFloat
    func roundTo(precision: Double) -> Double {
        return Double(self).roundTo(precision)
    }
}

extension Int {
    
    // calls the closure a number of times
    // e.g. 5.times { // do stuff }
    func times(closure: () -> ()) {
        if self > 0 {
            for _ in 1...self {
                closure()
            }
        }
    }
}

// calls the closure in the main queue
func queue(closure:()->()) {
    dispatch_async(dispatch_get_main_queue(), closure)
}

// calls the closure in the background
func background(closure:()->()) {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), closure)
}

// calls the closure after the delay
// e.g. delay(5.0) { // do stuff }
func delay(delay:Double, closure:()->()) {
    dispatch_after(
        dispatch_time(
            DISPATCH_TIME_NOW,
            Int64(delay * Double(NSEC_PER_SEC))
        ),
        dispatch_get_main_queue(), closure)
}

extension String {
    
    func titlecaseString() -> String {
        let words = self.componentsSeparatedByString(" ")
        var newWords = [String]()
        
        for word in words {
            let firstLetter = (word as NSString).substringToIndex(1)
            let restOfWord = (word as NSString).substringFromIndex(1)
            newWords.append("\(firstLetter.uppercaseString)\(restOfWord.lowercaseString)")
        }
        
        return (newWords as NSArray).componentsJoinedByString(" ")
    }
}

extension NSMutableString {
    
    // replaces all occurrences of one string with another
    func replace(target: String, with: String) {
        self.replaceOccurrencesOfString(target, withString: with, options: [], range: NSRange(location: 0, length: self.length))
    }
}
