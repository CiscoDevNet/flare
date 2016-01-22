//
//  AppDelegate.swift
//  Lights
//
//  Created by Andrew Zamler-Carhart on 1/22/16.
//  Copyright © 2016 Cisco. All rights reserved.
//

import Cocoa
import SocketIO
import Flare

@NSApplicationMain
class AppDelegate: NSObject, NSApplicationDelegate, FlareManagerDelegate {
    
    @IBOutlet weak var window: NSWindow!
    @IBOutlet weak var colorWell: NSColorWell!

    var defaults = NSUserDefaults.standardUserDefaults()
    var flareManager = FlareManager(host: "localhost", port: 1234)
    var lightManager = LightManager(hub: "hub", user: "user", light: 0)
    var thing: Thing?
    
    func applicationDidFinishLaunching(aNotification: NSNotification) {
        let path = NSBundle.mainBundle().pathForResource("Defaults", ofType: "plist")
        let factorySettings = NSDictionary(contentsOfFile: path!)
        
        defaults.registerDefaults(factorySettings! as! [String : AnyObject])
        
        loadFlare()
        loadLight()
    }
    
    @IBAction func loadFlare(sender: AnyObject) {
        loadFlare()
    }
    
    func loadFlare() {
        let host = defaults.stringForKey("host")
        let port = defaults.integerForKey("port")
        let thingId = defaults.stringForKey("thingId")
        
        if host != nil && thingId != nil {
            flareManager = FlareManager(host: host!, port: port)
            flareManager.delegate = self
            flareManager.debugHttp = false
            flareManager.debugSocket = true
            
            NSLog("Flare server: \(flareManager.server)")
            
            flareManager.connect()
            
            flareManager.getThing(thingId!, environmentId: "_", zoneId: "_") {json in
                self.thing = Thing(json: json)
                self.flareManager.flareIndex[self.thing!.id] = self.thing!
                NSLog("Thing: \(self.thing!.name)")
                self.flareManager.subscribe(self.thing!)
            }
        }
    }
    
    @IBAction func loadLight(sender: AnyObject) {
        loadLight()
    }
    
    func loadLight() {
        let hub = defaults.stringForKey("hub")
        let userId = defaults.stringForKey("userId")
        let lightId = defaults.integerForKey("lightId")
        
        if hub != nil && userId != nil {
            lightManager = LightManager(hub: hub!, user: userId!, light: lightId)

            NSLog("Light server: \(lightManager.server)")
            
            updateLight()
        }
    }
    
    func applicationWillTerminate(aNotification: NSNotification) {
        
    }

    func updateLight() {
        lightManager.getLight() { json in
            let on = json.getInt("on") == 1
            let hue = json.getInt("hue")
            let saturation = json.getInt("sat")
            let brightness = json.getInt("bri")
            
            self.defaults.setBool(on, forKey: "on")
            self.defaults.setInteger(hue, forKey: "hue")
            self.defaults.setInteger(saturation, forKey: "saturation")
            self.defaults.setInteger(brightness, forKey: "brightness")
            
            self.updateColorWell(hue, saturation: saturation, brightness: brightness)
        }
    }
    
    @IBAction func setPower(sender: AnyObject) {
        lightManager.setPower(defaults.boolForKey("on")) {jsonArray in }
        sendOn()
    }
    
    @IBAction func takeHue(sender: AnyObject) {
        takeColorComponents(sender)
        sendColor()
    }
    
    @IBAction func takeSaturation(sender: AnyObject) {
        takeColorComponents(sender)
    }
    
    @IBAction func takeBrightness(sender: AnyObject) {
        takeColorComponents(sender)
        sendBrightness()
    }
    
    @IBAction func takeColorComponents(sender: AnyObject) {
        let hue = defaults.integerForKey("hue")
        let saturation = defaults.integerForKey("saturation")
        let brightness = defaults.integerForKey("brightness")

        lightManager.setColor(hue, saturation: saturation, brightness: brightness) {jsonArray in }
        updateColorWell(hue, saturation: saturation, brightness: brightness)
    }
    
    func updateColorWell(hue: Int, saturation: Int, brightness: Int) {
        colorWell.color = NSColor(calibratedHue: CGFloat(hue) / 65535,
            saturation: CGFloat(saturation) / 255,
            brightness: CGFloat(brightness) / 255,
            alpha: 1.0)
    }
    
    @IBAction func takeColor(sender: NSColorWell) {
        setColor(sender.color)
    }
    
    func setColor(color: NSColor) {
        var hue: CGFloat = 0
        var saturation: CGFloat = 0
        var brightness: CGFloat = 0
        var alpha: CGFloat = 0
        
        color.getHue(&hue, saturation: &saturation, brightness: &brightness, alpha: &alpha)
        
        defaults.setInteger(Int(hue * 65535), forKey: "hue")
        defaults.setInteger(Int(saturation * 255), forKey: "saturation")
        defaults.setInteger(Int(brightness * 255), forKey: "brightness")
        
        takeColorComponents(self)
    }
    
    func didReceiveData(flare: Flare, data: JSONDictionary, sender: Flare?) {
        if flare == thing {
            if let color = data["color"] as? String {
                if let hue = hueForName(color) {
                    defaults.setInteger(hue, forKey: "hue")
                    takeColorComponents(self)
                }
            }
            
            if let brightness = data["brightness"] as? Float {
                defaults.setInteger(Int(brightness * 255.0), forKey: "brightness")
                takeColorComponents(self)
            }

            if let on = data["on"] as? Bool {
                defaults.setBool(on, forKey: "on")
                lightManager.setPower(defaults.boolForKey("on")) {jsonArray in }
            }
        }
    }
    
    func sendColor() {
        if thing != nil {
            let hue = defaults.integerForKey("hue")
            NSLog("Hue: \(hue)")
            if let color = nameForHue(hue) {
                flareManager.setData(thing!, key: "color", value: color, sender: thing)
            }
        }
    }
    
    func sendBrightness() {
        if thing != nil {
            let brightness = defaults.integerForKey("brightness")
            flareManager.setData(thing!, key: "brightness", value: Float(brightness) / 255, sender: thing)
        }
    }
    
    func sendOn() {
        if thing != nil {
            let on = defaults.boolForKey("on")
            flareManager.setData(thing!, key: "on", value: on, sender: thing)
        }
    }
    
    func hueForName(name: String) -> Int? {
        if name == "red" { return 0 }
        if name == "orange" { return 5461 }
        if name == "yellow" { return 10922 }
        if name == "green" { return 21845 }
        if name == "blue" { return 43690 }
        if name == "purple" { return 54612 }
        return nil
    }
    
    func nameForHue(hue: Int) -> String? {
        if hue == 0 || hue == 65535 { return "red" }
        if hue == 5461 { return "orange" }
        if hue == 10922 { return "yellow" }
        if hue == 21845 { return "green" }
        if hue == 43690 { return "blue" }
        if hue == 54612 { return "purple" }
        return nil
    }
    
    func colorForName(name: String) -> NSColor {
        if name == "red" { return NSColor.redColor() }
        if name == "orange" { return NSColor.orangeColor() }
        if name == "yellow" { return NSColor.yellowColor() }
        if name == "green" { return NSColor.greenColor() }
        if name == "blue" { return NSColor.blueColor() }
        if name == "purple" { return NSColor.purpleColor() }
        return NSColor.whiteColor()
    }
    
    func handleAction(flare: Flare, action: String, sender: Flare?) {
        
    }
    
    func near(thing: Thing, device: Device, distance: Double) {
        
    }
    
    func far(thing: Thing, device: Device) {
        
    }
}

extension Dictionary {
    func getInt(key: String) -> Int {
        if let value = self[key as! Key] as? Int {
            return value
        } else {
            return 0
        }
    }
}
