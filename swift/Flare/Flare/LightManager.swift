//
//  LightManager.swift
//  Lights
//
//  Created by Andrew Zamler-Carhart on 1/22/16.
//  Copyright © 2016 Cisco. All rights reserved.
//

import Foundation

public class LightManager: APIManager {
    
    public init(hub: String, user: String, light: Int) {
        super.init()
        
        self.server = "http://\(hub)/api/\(user)/lights/\(light)"
    }

    public func getLight(handler:(JSONDictionary) -> ()) {
        sendRequest("") {json in
            if let state = json["state"] as? JSONDictionary {
                handler(state)
            }
        }

    }
    
    public func setPower(on: Bool, handler:(JSONArray) -> ()) {
        sendRequest("state", params: nil, method: .PUT, message: ["on":on])
            {json in handler(json as! JSONArray)}
    }

    public func setColor(hue: Int, saturation: Int, brightness: Int, handler:(JSONArray) -> ()) {
        let message = ["hue":hue, "sat":saturation, "bri":brightness]
        sendRequest("state", params: nil, method: .PUT, message: message)
            {json in handler(json as! JSONArray)}
    }
    

}