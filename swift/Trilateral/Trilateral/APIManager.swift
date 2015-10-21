//
//  APIManager.swift
//  Facets Dashboard
//
//  Created by Andrew Zamler-Carhart on 29/08/2014.
//  Copyright (c) 2014 Cisco. All rights reserved.
//

import Foundation

typealias JSONDictionary = [String:AnyObject]
typealias JSONArray = [JSONDictionary]

class APIManager: NSObject {
    
    var server = "http://localhost:80"
    
    enum HTTPMethod: String {
        case GET = "GET"
        case POST = "POST"
        case PUT = "PUT"
        case DELETE = "DELETE"
    }
    
    // takes a uri, params, method and message
    // handler called with json and other info
    func sendRequest(uri: String,
        params: [String:String]?,
        method: HTTPMethod,
        message: JSONDictionary?,
        handler:(JSONDictionary, NSURLRequest, NSURLResponse?, Double) -> ())
    {
        let startTime = NSDate()
        let url = urlWithParams(uri, params:params)
        let request = NSMutableURLRequest(URL:url)
        request.HTTPMethod = method.rawValue
        
        if (message != nil) {
            let messageData = message!.toData()
            if messageData != nil { request.HTTPBody = messageData }
            request.addValue("application/json", forHTTPHeaderField: "Content-Type")
            
            // let messageString = NSString(data: messageData!, encoding: NSUTF8StringEncoding)
            // NSLog("url: \(request.HTTPMethod) \(url)")
            // NSLog("message: \(messageString)");
        }
        
        NSURLSession.sharedSession().dataTaskWithRequest(request) { (data: NSData?, response: NSURLResponse?, error: NSError?) -> Void in
            if error == nil {
                if let json = data!.toJSONDictionary() {
                    let duration = 0 - startTime.timeIntervalSinceNow
                    handler(json, request, response, duration)
                } else {
                    NSLog("Not json: \(NSString(data: data!, encoding: NSUTF8StringEncoding)))")
                }
            } else {
                NSLog("Error: \(error!.localizedDescription)")
            }
        }
    }
    
    // takes a uri, params, method and message
    // handler called with json only
    func sendRequest(uri: String,
        params: [String:String]?,
        method: HTTPMethod,
        message: JSONDictionary?,
        handler:(JSONDictionary) -> ())
    {
        sendRequest(uri, params:params, method:method, message:message) {(json, request, response, duration)->() in handler(json) }
    }
    
    // GET request, takes a uri and params
    // handler called with json and other info
    func sendRequest(uri: String,
        params: [String:String]?,
        longHandler:(JSONDictionary, NSURLRequest, NSURLResponse?, Double) -> ())
    {
        sendRequest(uri, params:params, method:.GET, message:nil, handler:longHandler)
    }
    
    // GET request, takes a uri and params
    // handler called with json only
    func sendRequest(uri: String, params: [String:String]?, handler:(JSONDictionary) -> ()) {
        sendRequest(uri, params:params, method:.GET, message:nil, handler:handler)
    }
    
    // GET request, takes a uri
    // handler called with json and other info
    func sendRequest(uri: String, longHandler:(JSONDictionary, NSURLRequest, NSURLResponse?, Double) -> ()) {
        sendRequest(uri, params:nil, longHandler:longHandler)
    }
    
    // GET request, takes a uri
    // handler called with json only
    func sendRequest(uri: String, handler:(JSONDictionary) -> ()) {
        sendRequest(uri, params:nil, handler:handler)
    }
    
    // returns a fully-qualified URL with the given uri and parameters
    func urlWithParams(uri: String, params: [String:String]?) -> NSURL {
        var urlString = server + "/" + uri
        if params != nil && params!.count > 0 {
            urlString += "?" + paramString(params!)
        }
        let url: NSURL? = NSURL(string:urlString)
        return url!
    }
    
    // formats the parameters for a URL
    func paramString(params: [String:String]) -> String {
        let keyValues = NSMutableArray()
        for (key, value) in params { keyValues.addObject(key + "=" + value) }
        return keyValues.componentsJoinedByString("&")
    }
    
    func jsonDict(json: AnyObject?) -> JSONDictionary {
        if let dict = json as? JSONDictionary {
            return dict
        }
        return [:]
    }
    
    // this handler can be used to print out all info from an API call
    var printInfo = {(json: JSONDictionary, request: NSURLRequest, response: NSURLResponse?, duration: Double) -> () in
        print("url: \(request.HTTPMethod) \(request.URL)")
        if response != nil { print("response: \((response! as! NSHTTPURLResponse).statusCode)") }
        print("json: \(json)")
        print("duration: \(duration)")
    }
    
    // this handler can be used to print out the json from an API call
    var printJson = {(json: JSONDictionary) -> () in
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

/*extension Dictionary: JSONValue {
init() {
self.init(minimumCapacity:10)
}

init(string: String) {
self.init(minimumCapacity:10)
}
}*/

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
    
    func getDictionary(key: String) -> JSONDictionary {
        if let value = self[key as! Key] {
            return value as! JSONDictionary
        } else {
            return [:]
        }
    }
}
