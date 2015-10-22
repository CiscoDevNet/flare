//
//  APIManager.swift
//  Facets Dashboard
//
//  Created by Andrew Zamler-Carhart on 29/08/2014.
//  Copyright (c) 2014 Cisco. All rights reserved.
//

import Foundation

public typealias JSONDictionary = [String:AnyObject]
public typealias JSONArray = [JSONDictionary]

public class APIManager: NSObject {
    
    public var server = "http://localhost:80"
    public var debugHttp = false

    public enum HTTPMethod: String {
        case GET = "GET"
        case POST = "POST"
        case PUT = "PUT"
        case DELETE = "DELETE"
    }
    
    // takes a uri, params, method and message
    // handler called with json and other info
    public func sendRequestDetailed(uri: String,
        params: JSONDictionary?,
        method: HTTPMethod,
        message: JSONDictionary?,
        handler:(AnyObject, NSURLRequest, NSURLResponse?, Double) -> ())
    {
        let startTime = NSDate()
        let url = urlWithParams(uri, params:params)
        let request = NSMutableURLRequest(URL:url)
        request.HTTPMethod = method.rawValue
        
        if debugHttp { NSLog("url: \(request.HTTPMethod) \(url)") }
        
        if (message != nil) {
            let messageData = try? NSJSONSerialization.dataWithJSONObject(message!, options:[])
            let messageString = NSString(data: messageData!, encoding: NSUTF8StringEncoding)
            
            request.HTTPBody = messageData
            request.addValue("application/json", forHTTPHeaderField: "Content-Type")
            
            if debugHttp { NSLog("message: \(messageString)") }
        }
        
        NSURLConnection.sendAsynchronousRequest(request, queue: NSOperationQueue.mainQueue(),
            completionHandler: {(response: NSURLResponse?, data: NSData?, error: NSError?) -> () in
                if error == nil {
                    let duration = 0 - startTime.timeIntervalSinceNow
                    if let json: AnyObject = try? NSJSONSerialization.JSONObjectWithData(data!, options: [])  {
                        handler(json, request, response, duration)
                    } else {
                        NSLog("Not json: \(NSString(data: data!, encoding: NSUTF8StringEncoding)))")
                        handler([:], request, response, duration)
                    }
                } else {
                    NSLog("Error: \(error!.localizedDescription)")
                }
        })
    }
    
    // takes a uri, params, method and message
    // handler called with json only
    public func sendRequest(uri: String,
        params: JSONDictionary?,
        method: HTTPMethod,
        message: JSONDictionary?,
        handler:(AnyObject) -> ())
    {
        sendRequestDetailed(uri, params:params, method:method, message:message) {(json, request, response, duration)->() in handler(json) }
    }
    
    // GET request, takes a uri and params
    // handler called with json and other info
    public func sendRequestDetailed(uri: String,
        params: JSONDictionary?,
        handler:(AnyObject, NSURLRequest, NSURLResponse?, Double) -> ())
    {
        sendRequestDetailed(uri, params:params, method:.GET, message:nil, handler:handler)
    }
    
    // GET request, takes a uri and params
    // handler called with json only
    public func sendRequest(uri: String, params: JSONDictionary?, handler:(AnyObject) -> ()) {
        sendRequest(uri, params:params, method:.GET, message:nil, handler:handler)
    }
    
    // GET request, takes a uri
    // handler called with json and other info
    public func sendRequestDetailed(uri: String, handler:(AnyObject, NSURLRequest, NSURLResponse?, Double) -> ()) {
        sendRequestDetailed(uri, params:nil, handler:handler)
    }
    
    // GET request, takes a uri
    // handler called with json only
    public func sendRequest(uri: String, handler:(AnyObject) -> ()) {
        sendRequest(uri, params:nil, handler:handler)
    }
    
    // returns a fully-qualified URL with the given uri and parameters
    public func urlWithParams(uri: String, params: JSONDictionary?) -> NSURL {
        var urlString = server + "/" + uri
        if params != nil && params!.count > 0 {
            urlString += "?" + paramString(params!)
        }
        let url: NSURL? = NSURL(string:urlString)
        return url!
    }
    
    // formats the parameters for a URL
    public func paramString(params: JSONDictionary) -> String {
        let keyValues = NSMutableArray()
        for (key, value) in params { keyValues.addObject("\(key)=\(value)") }
        return keyValues.componentsJoinedByString("&")
    }
    
    public func jsonDict(json: AnyObject?) -> JSONDictionary {
        if let dict = json as? JSONDictionary {
            return dict
        }
        return [:]
    }
    
    // this handler can be used to print out all info from an API call
    public var printInfo = {(json: JSONDictionary, request: NSURLRequest, response: NSURLResponse?, duration: Double) -> () in
        print("url: \(request.HTTPMethod) \(request.URL)")
        if response != nil { print("response: \((response! as! NSHTTPURLResponse).statusCode)") }
        print("json: \(json)")
        print("duration: \(duration)")
    }
    
    // this handler can be used to print out the json from an API call
    public var printJson = {(json: JSONDictionary) -> () in
        print("json: \(json)")
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
