//
//  Extensions.swift
//  Facets Dashboard
//
//  Created by Andrew Zamler-Carhart on 16/09/2014.
//  Copyright (c) 2014 Cisco. All rights reserved.
//

import Foundation
import CoreGraphics

public typealias JSONDictionary = [String:AnyObject]
public typealias JSONArray = [JSONDictionary]

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
    let width = randomInt(min, max: max)
    let height = randomInt(min, max: max)
    return CGSize(width:width, height:height)
}

extension Array {
    
    // returns a random object from the array
    func randomObject() -> Element  {
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

extension CGRect {
    
    // returns the point at the center of the rectangle
    func center() -> CGPoint {
        return CGPoint(x: (self.minX + self.maxX) / 2.0,
            y: (self.minY + self.maxY) / 2.0)
    }

    public func toJSON() -> JSONDictionary {
        return ["origin": self.origin.toJSON(), "size": self.size.toJSON()]
    }
}

public extension CGSize {
    
    // returns the diagonal length
    public func length() -> Double {
        let x2 = Double(self.width * self.width)
        let y2 = Double(self.height * self.height)
        return sqrt(x2 + y2)
    }
    
    public func toJSON() -> JSONDictionary {
        return ["width": self.width, "height": self.height]
    }
}

public extension CGPoint {
    
    // rounds the point's x and y values to the given precision
    public func roundTo(precision: Double) -> CGPoint {
        return CGPoint(x: self.x.roundTo(precision), y: self.y.roundTo(precision))
    }
    
    public func toJSON() -> JSONDictionary {
        return ["x": self.x, "y": self.y]
    }
}

public extension CGFloat {
    
    // adds the same rounding behavior to CGFloat
    public func roundTo(precision: Double) -> Double {
        return Double(self).roundTo(precision)
    }
}

func radiansToDegrees(radians: Double) -> Double {
    return radians * 180.0 / M_PI;
}

public extension Int {
    
    // calls the closure a number of times
    // e.g. 5.times { // do stuff }
    public func times(closure: () -> ()) {
        if self > 0 {
            for _ in 1...self {
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

public extension String {
    
    public func titlecaseString() -> String {
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

public extension NSMutableString {
    
    // replaces all occurrences of one string with another
    public func replace(target: String, with: String) {
        self.replaceOccurrencesOfString(target, withString: with, options: [], range: NSRange(location: 0, length: self.length))
    }
}

public extension NSData {

    // converts NSData to an NSDictionary
    public func toJSONDictionary() -> JSONDictionary? {
        let json: JSONDictionary?
        do {
            json = try NSJSONSerialization.JSONObjectWithData(self, options: []) as? JSONDictionary
        } catch _ {
            json = nil
        }
        return json
    }
}

public extension Dictionary {

    // converts a JSONDictionary to NSData
    public func toData() -> NSData? {
        let data: NSData?
        do {
            data = try NSJSONSerialization.dataWithJSONObject(self as! AnyObject, options:[])
        } catch _ {
            data = nil
        }
        return data
    }
}

// objects that can be initialized with no arguments
// necessary for getValue() to instantiate a new object of arbitrary type
// most classes can implement this protocol with no additional methods
protocol JSONValue {
    init()
    init(string: String)
}

// adds Initible conformace to basic types so they can be used by getValue()
extension String: JSONValue {
    init(string: String) {
        self.init(string)
    }
}

extension Int: JSONValue {
    init(string: String) {
        self.init((string as NSString).integerValue)
    }
}
extension Double: JSONValue {
    init(string: String) {
        self.init((string as NSString).doubleValue)
    }
}
extension Array: JSONValue {
    init(string: String) {
        self.init()
    }
}

extension Dictionary {
    
    // getValue will try to get an object with the given key from the dictionary
    // returns an object of the given type rather than AnyObject?
    // if the object is not found, returns a new object of the type
    func getValue<T: JSONValue>(key: String, type: T.Type) -> T {
        if let value = self[key as! Key] {
            if let typedValue = value as? T {
                return typedValue
            } else if let stringValue = value as? String {
                // NSLog("Got string: \(value)")
                return T(string:stringValue)
            }
        }
        
        return T()
    }
    
    func getString(key: String) -> String {
        return getValue(key, type: String.self)
    }
    
    func getInt(key: String) -> Int {
        return getValue(key, type: Int.self)
    }
    
    func getDouble(key: String) -> Double {
        return getValue(key, type: Double.self)
    }
    
    func getArray(key: String) -> JSONArray {
        return getValue(key, type: JSONArray.self)
    }
    
    func getDate(key: String) -> NSDate {
        let dateString = getString(key)
        let mongoFormatter = NSDateFormatter()
        mongoFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        if let date = mongoFormatter.dateFromString(dateString) {
            return date
        } else {
            return NSDate()
        }
    }
    
    func getDictionary(key: String) -> JSONDictionary {
        if let value = self[key as! Key] {
            return value as! JSONDictionary
        } else {
            return [:]
        }
    }
}

