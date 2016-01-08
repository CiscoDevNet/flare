//
//  AppDelegate.swift
//  Flare Test
//
//  Created by Andrew Zamler-Carhart on 3/23/15.
//  Copyright (c) 2015 Andrew Zamler-Carhart. All rights reserved.
//

import Cocoa
import Flare

@NSApplicationMain
class AppDelegate: NSObject, NSApplicationDelegate, FlareManagerDelegate {
    
    @IBOutlet weak var window: NSWindow!
    @IBOutlet weak var mapWindow: NSWindow!
    @IBOutlet weak var compassWindow: NSWindow!
    @IBOutlet weak var outlineView: NSOutlineView!
    @IBOutlet weak var tabView: NSTabView!
    @IBOutlet weak var map: IndoorMap!
    @IBOutlet weak var compass: CompassView!
    
    @IBOutlet weak var idField: NSTextField!
    @IBOutlet weak var nameField: NSTextField!
    @IBOutlet weak var commentField: NSTextField!

    @IBOutlet weak var uuidField: NSTextField!
    @IBOutlet weak var environmentXField: NSTextField!
    @IBOutlet weak var environmentYField: NSTextField!
    @IBOutlet weak var environmentWidthField: NSTextField!
    @IBOutlet weak var environmentHeightField: NSTextField!
    @IBOutlet weak var environmentAngleField: NSTextField!
    @IBOutlet weak var latitudeField: NSTextField!
    @IBOutlet weak var longitudeField: NSTextField!
    @IBOutlet weak var radiusField: NSTextField!
    
    @IBOutlet weak var majorField: NSTextField!
    @IBOutlet weak var zoneXField: NSTextField!
    @IBOutlet weak var zoneYField: NSTextField!
    @IBOutlet weak var zoneWidthField: NSTextField!
    @IBOutlet weak var zoneHeightField: NSTextField!

    @IBOutlet weak var minorField: NSTextField!
    @IBOutlet weak var colorField: NSTextField!
    @IBOutlet weak var brightnessField: NSTextField!
    @IBOutlet weak var thingXField: NSTextField!
    @IBOutlet weak var thingYField: NSTextField!

    @IBOutlet weak var angleField: NSTextField!
    @IBOutlet weak var deviceXField: NSTextField!
    @IBOutlet weak var deviceYField: NSTextField!

    @IBOutlet weak var nearbyDeviceView: NSView!
    @IBOutlet weak var nearbyDeviceIdField: NSTextField!
    @IBOutlet weak var nearbyDeviceNameField: NSTextField!
    @IBOutlet weak var nearbyDeviceCommentField: NSTextField!
    @IBOutlet weak var nearbyDeviceDistanceField: NSTextField!
    @IBOutlet weak var nearbyDeviceAngleField: NSTextField!
    
    @IBOutlet weak var nearbyThingView: NSView!
    @IBOutlet weak var nearbyThingIdField: NSTextField!
    @IBOutlet weak var nearbyThingNameField: NSTextField!
    @IBOutlet weak var nearbyThingCommentField: NSTextField!
    @IBOutlet weak var nearbyThingDistanceField: NSTextField!
    @IBOutlet weak var nearbyThingColorField: NSTextField!
    @IBOutlet weak var nearbyThingBrightnessField: NSTextField!
    
    @IBOutlet weak var mapDirectionButtons: NSView!
    @IBOutlet weak var compassDirectionButtons: NSView!

    
    var flareManager = FlareManager(host: "localhost", port: 1234)
    var environments = [Environment]()
    var selectedFlare, nearbyFlare: Flare?
    var defaults: NSUserDefaults
    
    let animationDelay = 0.5
    let animationSteps = 30
    
    override init() {
        defaults = NSUserDefaults.standardUserDefaults()
        
        super.init()
    }
    
    func applicationDidFinishLaunching(aNotification: NSNotification) {
        let path = NSBundle.mainBundle().pathForResource("Defaults", ofType: "plist")
        let factorySettings = NSDictionary(contentsOfFile: path!)
        let defaultsController = NSUserDefaultsController.sharedUserDefaultsController()
        
        defaults.registerDefaults(factorySettings! as! [String : AnyObject])

        defaultsController.addObserver(self, forKeyPath: "values.host", options: [], context: nil)
        defaultsController.addObserver(self, forKeyPath: "values.port", options: [], context: nil)

        load()
    }
    
    func load() {
        let host = defaults.stringForKey("host")!
        let port = defaults.integerForKey("port")
        
        flareManager = FlareManager(host: host, port: port)
        flareManager.delegate = self
        flareManager.debugHttp = false
        flareManager.debugSocket = true
        
        NSLog("Flare server: \(flareManager.server)")
        
        flareManager.connect()
        
        let selected = defaults.stringForKey("selectedId")
        loadEnvironments(selected)
    }

    @IBAction func refresh(sender: AnyObject) {
        let selected = defaults.stringForKey("selectedId")
        loadEnvironments(selected)
    }
    
    func loadEnvironments(selectId: String?) {
        flareManager.loadEnvironments() {(environments)->() in
            self.environments = environments
            // self.printEnvironments()
            self.outlineView.reloadData()
            self.expandAll()
            
            if selectId != nil {
                if let newSelected = self.flareManager.flareIndex[selectId!] {
                    let row = self.outlineView.rowForItem(newSelected)
                    if row != NSNotFound {
                        self.outlineView.selectRowIndexes(NSIndexSet(index: row), byExtendingSelection: false)
                        self.outlineView.scrollRowToVisible(row)
                    }
                }
            }
        }
    }
    
    func applicationWillTerminate(aNotification: NSNotification) {
        // Insert code here to tear down your application
    }
    
    override func observeValueForKeyPath(keyPath: String?, ofObject object: AnyObject?,
        change: [String : AnyObject]?, context: UnsafeMutablePointer<Void>)
    {
        switch keyPath! {
            case "values.host", "values.port": load()
            default: break
        }
    }

    func printEnvironments() {
        for environment in environments {
            NSLog("\(environment)")
            
            for zone in environment.zones {
                NSLog("  \(zone)")
                
                for thing in zone.things {
                    NSLog("    \(thing)")
                }
            }
            
            if environment.devices.count > 0 {
                NSLog("  Devices")
                for device in environment.devices {
                    NSLog("    \(device)")
                }
            }
        }
    }
    
    func expandAll() {
        for environment in environments {
            outlineView.expandItem(environment)
            
            for zone in environment.zones {
                outlineView.expandItem(zone)
            }
        }
    }
    
    @IBAction func addRemove(sender: NSSegmentedControl) {
        let selected = sender.selectedSegment

        switch selected {
        case 0: newFlare(sender)
        case 1: deleteFlare(sender)
        default: break
        }
    }
    
    @IBAction func newFlare(sender: AnyObject) {
        let template: JSONDictionary = ["name":"Untitled", "description":"", "data":[:]]

        flareManager.newFlare(selectedFlare, json: template) { json in
            if let selectId = json["_id"] as? String {
                self.loadEnvironments(selectId)
            }
        }
    }
    
    @IBAction func deleteFlare(sender: AnyObject) {
        if let flare = selectedFlare {
            let alert = NSAlert()
            alert.messageText = "Do you want to delete “\(flare.name)”?"
            alert.addButtonWithTitle("Delete")
            alert.addButtonWithTitle("Cancel")
            alert.informativeText = "This operation cannot be undone."
            
            alert.beginSheetModalForWindow(self.window, completionHandler: { (returnCode) -> Void in
                if returnCode == NSAlertFirstButtonReturn {
                    let parentId = flare.parentId()
                    self.flareManager.deleteFlare(flare) { json in
                        self.loadEnvironments(parentId)
                    }
                }
            })
        }

        
    }
    
    // MARK: Callbacks
    
    func didReceiveData(flare: Flare, data: JSONDictionary, sender: Flare?) {
        NSLog("\(flare.name) data: \(data)")
        
        if flare == selectedFlare {
            if let color = data["color"] as? String {
                colorField.stringValue = color
            }
            
            if let brightness = data["brightness"] as? Double {
                brightnessField.doubleValue = brightness
            }

            if let angle = data["angle"] as? Double {
                angleField.doubleValue = angle
            }
        } else if flare == nearbyFlare {
            if let angle = data["angle"] as? Double {
                nearbyDeviceAngleField.doubleValue = angle
            }

            if let color = data["color"] as? String {
                nearbyThingColorField.stringValue = color
            }

            if let brightness = data["brightness"] as? Double {
                nearbyThingBrightnessField.doubleValue = brightness
            }
        }
        
        map.dataChanged()
        compass.dataChanged()
    }
    
    func didReceivePosition(flare: Flare, oldPosition: CGPoint, newPosition: CGPoint, sender: Flare?) {
        // NSLog("\(flare.name) position: \(newPosition)")
        
        if flare == selectedFlare {
            if flare is Thing {
                thingXField.doubleValue = Double(newPosition.x)
                thingYField.doubleValue = Double(newPosition.y)
            } else if flare is Device {
                deviceXField.doubleValue = Double(newPosition.x)
                deviceYField.doubleValue = Double(newPosition.y)
            }
        } else if flare == nearbyFlare {
            if let device = nearbyFlare as? Device, thing = selectedFlare as? Thing {
                let distance = device.position - thing.position
                nearbyDeviceDistanceField.doubleValue = distance
            } else if let thing = nearbyFlare as? Thing, device = selectedFlare as? Device {
                let distance = thing.position - device.position
                nearbyThingDistanceField.doubleValue = distance
            }
        }

        animateFlare(flare as! FlarePosition, oldPosition: oldPosition, newPosition: newPosition)
    }
    
    func handleAction(flare: Flare, action: String, sender: Flare?) {
        NSLog("\(flare.name) action: \(action)")
    }
    
    func enter(zone: Zone, device: Device) {
        NSLog("\(zone.name) enter: \(device.name)")
    }
    
    func exit(zone: Zone, device: Device) {
        NSLog("\(zone.name) exit: \(device.name)")
    }
    
    func near(thing: Thing, device: Device, distance: Double) {
        NSLog("\(thing.name) near: \(device.name) (\(distance))")
        
        if selectedFlare == thing && nearbyFlare != device {
            nearbyFlare = device
            
            flareManager.subscribe(device)
            flareManager.getData(device)
            flareManager.getPosition(device)
            
            nearbyDeviceIdField.stringValue = device.id
            nearbyDeviceNameField.stringValue = device.name
            nearbyDeviceCommentField.stringValue = device.comment
            nearbyDeviceDistanceField.doubleValue = distance
            if let angle = device.data["angle"] as? Double {
                nearbyDeviceAngleField.doubleValue = angle
            }
            
            nearbyDeviceView.hidden = false
        } else if selectedFlare == device && nearbyFlare != thing {
            nearbyFlare = thing
            map.nearbyThing = thing
            compass.nearbyThing = thing
            
            flareManager.subscribe(thing)
            flareManager.getData(thing)
            flareManager.getPosition(thing)

            nearbyThingIdField.stringValue = thing.id
            nearbyThingNameField.stringValue = thing.name
            nearbyThingCommentField.stringValue = thing.comment
            nearbyThingDistanceField.doubleValue = distance
            if let color = thing.data["color"] as? String {
                nearbyThingColorField.stringValue = color
            }
            if let brightness = thing.data["brightness"] as? Double {
                nearbyThingBrightnessField.doubleValue = brightness
            }
            
            nearbyThingView.hidden = false
}
    }
    
    func far(thing: Thing, device: Device) {
        NSLog("\(device.name) far: \(thing.name)")
        
        if selectedFlare == thing && nearbyFlare == device {
            nearbyDeviceView.hidden = true
            
            flareManager.unsubscribe(device)
            
            nearbyFlare = nil
        } else if selectedFlare == device && nearbyFlare == thing {
            nearbyThingView.hidden = true
            
            flareManager.unsubscribe(thing)
            
            nearbyFlare = nil
            map.nearbyThing = nil
            compass.nearbyThing = nil
        }
    }
    
    // MARK: Actions
    
    @IBAction func changeName(sender: NSTextField) {
        if let flare = selectedFlare {
            let name = sender.stringValue
            flare.name = name
            self.outlineView.reloadData()
            flareManager.updateFlare(flare, json: ["name":name]) {json in }
        }
    }
    
    @IBAction func changeComment(sender: NSTextField) {
        if let flare = selectedFlare {
            let comment = sender.stringValue
            flare.comment = comment
            flareManager.updateFlare(flare, json: ["description":comment]) {json in }
        }
    }
    
    // sender.identifier can contain several words
    // the first word is the key
    // if the words contains "nearby", sends the message for the nearby flare rather than the selected flare
    // if the words contains "integer" or "double", formats the value as a number
    @IBAction func changeData(sender: NSTextField) {
        let identifiers = sender.identifier!.componentsSeparatedByString(" ")
        let key = identifiers.first!
        let nearby = identifiers.contains("nearby")
        
        var value: AnyObject? = nil
        if identifiers.contains("integer") {
            value = sender.integerValue
        } else if identifiers.contains("double") {
            value = sender.doubleValue
        } else {
            value = sender.stringValue
        }
        
        if let flare = nearby ? nearbyFlare : selectedFlare {
            flare.data[key] = value!
            flareManager.setData(flare, key: key, value: value!, sender: nil)
            
            map.dataChanged()
            compass.dataChanged()
        }
    }
    
    @IBAction func changePosition(sender: NSTextField) {
        if let thing = selectedFlare as? Thing {
            let newPosition = CGPoint(x: thingXField.doubleValue, y: thingYField.doubleValue)
            animateFlare(thing, oldPosition: thing.position, newPosition: newPosition)
            flareManager.setPosition(thing, position: newPosition, sender: nil)
        } else if let device = selectedFlare as? Device {
            let newPosition = CGPoint(x: deviceXField.doubleValue, y: deviceYField.doubleValue)
            animateFlare(device, oldPosition: device.position, newPosition: newPosition)
            flareManager.setPosition(device, position: newPosition, sender: nil)
        }
    }

    func animateFlare(var flare: FlarePosition, oldPosition: CGPoint, newPosition: CGPoint) {
        let dx = (newPosition.x - oldPosition.x) / CGFloat(animationSteps)
        let dy = (newPosition.y - oldPosition.y) / CGFloat(animationSteps)

        delayLoop(animationDelay, steps: animationSteps) { i in
            flare.position = CGPoint(x: oldPosition.x + CGFloat(i) * dx,
                                     y: oldPosition.y + CGFloat(i) * dy)
            self.map.dataChanged()
            self.compass.dataChanged()
        }
    }
    
    @IBAction func changeGeofence(sender: NSTextField) {
        if let environment = selectedFlare as? Environment {
            let geofence = ["latitude":latitudeField.doubleValue,
                            "longitude":longitudeField.doubleValue,
                            "radius":radiusField.doubleValue]
            environment.geofence = Geofence(json: geofence)
            flareManager.updateFlare(environment, json: ["geofence":geofence]) {json in }
        }
    }
    
    @IBAction func changeEnvironmentAngle(sender: NSTextField) {
        if let environment = selectedFlare as? Environment {
            let angle = environmentAngleField.doubleValue
            environment.angle = angle
            flareManager.updateFlare(environment, json: ["angle":angle]) {json in }
        }
    }
    
    @IBAction func changeEnvironmentPerimeter(sender: NSTextField) {
        if let environment = selectedFlare as? Environment {
            let perimeter = ["origin":["x":environmentXField.doubleValue, "y":environmentYField.doubleValue],
                "size":["width":environmentWidthField.doubleValue, "height":environmentHeightField.doubleValue]]
            environment.perimeter = getRect(perimeter)
            map.dataChanged()
            flareManager.updateFlare(environment, json: ["perimeter":perimeter]) {json in }
        }
    }
    
    @IBAction func changeZonePerimeter(sender: NSTextField) {
        if let zone = selectedFlare as? Zone {
            let perimeter = ["origin":["x":zoneXField.doubleValue, "y":zoneYField.doubleValue],
                "size":["width":zoneWidthField.doubleValue, "height":zoneHeightField.doubleValue]]
            zone.perimeter = getRect(perimeter)
            map.dataChanged()
            flareManager.updateFlare(zone, json: ["perimeter":perimeter]) {json in }
        }
    }
    
    // sender.identifier can contain several words
    // the first word is the action
    // if the words contains "nearby", sends the message for the nearby flare rather than the selected flare
    @IBAction func performAction(sender: NSButton) {
        let identifiers = sender.identifier!.componentsSeparatedByString(" ")
        let action = identifiers.first!
        let nearby = identifiers.contains("nearby")
        
        if let flare = nearby ? nearbyFlare : selectedFlare {
            flareManager.performAction(flare, action: action, sender: nil)
        }
    }
    
    // MARK: Outline View
    
    func reloadData() {
        outlineView.reloadData()
    }
    
    func reloadItem(item: Flare) {
        outlineView.reloadItem(item, reloadChildren: true)
        
        if item.children().count > 0 {
            outlineView.expandItem(item)
        } else {
            outlineView.collapseItem(item)
        }
    }
    
    func selectedItem() -> Flare? {
        if let flare = outlineView.itemAtRow(outlineView.selectedRow) as? Flare { return flare }
        return nil
    }
    
    func outlineView(outlineView: NSOutlineView!, numberOfChildrenOfItem item: AnyObject!) -> Int {
        if let environment = item as? Environment {
            return environment.zones.count + environment.devices.count
        } else {
            return item == nil ? environments.count : (item as! Flare).children().count
        }
    }
    
    func outlineView(outlineView: NSOutlineView!, child index: Int, ofItem item: AnyObject!) -> AnyObject! {
        if let environment = item as? Environment {
            let zoneCount = environment.zones.count
            return index < zoneCount ? environment.zones[index] : environment.devices[index - zoneCount]
        } else {
            return item == nil ? environments[index] : (item as! Flare).children()[index]
        }
    }
    
    func outlineView(outlineView: NSOutlineView!, isItemExpandable item: AnyObject!) -> Bool {
        return !(item is Thing || item is Device)
    }
    
    func outlineView(outlineView: NSOutlineView!, viewForTableColumn tableColumn: NSTableColumn!, item: AnyObject!) -> NSView! {
        let view = (item as! Flare).id == "" ?
            outlineView.makeViewWithIdentifier("HeaderCell", owner: self) as! NSTableCellView :
            outlineView.makeViewWithIdentifier("DataCell", owner: self) as! NSTableCellView
        
        view.textField!.stringValue = (item as! Flare).name
        
        if item is Environment {
            view.imageView!.image = NSImage(named: "NSHomeTemplate")
        } else if item is Zone {
            view.imageView!.image = NSImage(named: "NSIChatTheaterTemplate")
        } else if item is Thing {
            view.imageView!.image = NSImage(named: "NSActionTemplate")
        } else if item is Device {
            view.imageView!.image = NSImage(named: "NSComputer")
        }
        
        return view
    }
    
    func outlineViewSelectionDidChange(notification: NSNotification!) {
        // should use willset, didset
        
        if let oldFlare = selectedFlare {
            flareManager.unsubscribe(oldFlare)
            
            idField.stringValue = ""
            nameField.stringValue = ""
            commentField.stringValue = ""
            uuidField.stringValue = ""
            environmentXField.stringValue = ""
            environmentYField.stringValue = ""
            environmentWidthField.stringValue = ""
            environmentHeightField.stringValue = ""
            environmentAngleField.stringValue = ""
            latitudeField.stringValue = ""
            longitudeField.stringValue = ""
            radiusField.stringValue = ""
            majorField.stringValue = ""
            zoneXField.stringValue = ""
            zoneYField.stringValue = ""
            zoneWidthField.stringValue = ""
            zoneHeightField.stringValue = ""
            minorField.stringValue = ""
            colorField.stringValue = ""
            brightnessField.stringValue = ""
            thingXField.stringValue = ""
            thingYField.stringValue = ""
            angleField.stringValue = ""
            deviceXField.stringValue = ""
            deviceYField.stringValue = ""
        }
        
        selectedFlare = selectedItem()
        map.selectedFlare = selectedFlare
        nearbyDeviceView.hidden = true
        nearbyThingView.hidden = true
        nearbyFlare = nil
        
        if let newFlare = selectedFlare {
            defaults.setObject(newFlare.id, forKey: "selectedId")
            
            flareManager.subscribe(newFlare, all: true)
            flareManager.getData(newFlare)
            
            idField.stringValue = newFlare.id
            nameField.stringValue = newFlare.name
            commentField.stringValue = newFlare.comment

            if let environment = flareManager.environmentForFlare(newFlare) {
                self.map.loadEnvironment(environment)

                if compass.device == nil {
                    NSLog("Devices: \(environment.devices)")
                    if let device = environment.devices.first {
                        compass.environment = environment
                        compass.device = device
                        compass.dataChanged()
                    }
                }
            }
            
            compass.selectedThing = nil
            
            if let environment = selectedFlare as? Environment {
                tabView.selectTabViewItemAtIndex(0)
                mapDirectionButtons.hidden = true
                compassDirectionButtons.hidden = true
                
                if let uuid = environment.data["uuid"] as? String { uuidField.stringValue = uuid }
                environmentXField.doubleValue = Double(environment.perimeter.origin.x)
                environmentYField.doubleValue = Double(environment.perimeter.origin.y)
                environmentWidthField.doubleValue = Double(environment.perimeter.size.width)
                environmentHeightField.doubleValue = Double(environment.perimeter.size.height)
                environmentAngleField.doubleValue = Double(environment.angle)
                latitudeField.doubleValue = Double(environment.geofence.latitude)
                longitudeField.doubleValue = Double(environment.geofence.longitude)
                radiusField.doubleValue = Double(environment.geofence.radius)
            } else if let zone = selectedFlare as? Zone {
                NSLog("Selected \(zone)")
                tabView.selectTabViewItemAtIndex(1)
                mapDirectionButtons.hidden = true
                compassDirectionButtons.hidden = true
                
                if let major = zone.data["major"] as? Int { majorField.integerValue = major }
                
                zoneXField.doubleValue = Double(zone.perimeter.origin.x)
                zoneYField.doubleValue = Double(zone.perimeter.origin.y)
                zoneWidthField.doubleValue = Double(zone.perimeter.size.width)
                zoneHeightField.doubleValue = Double(zone.perimeter.size.height)
            } else if let thing = selectedFlare as? Thing {
                NSLog("Selected \(thing)")
                tabView.selectTabViewItemAtIndex(2)
                mapDirectionButtons.hidden = false
                compassDirectionButtons.hidden = false

                if let minor = thing.data["minor"] as? Int { minorField.integerValue = minor }

                flareManager.getPosition(thing)
                
                if let color = thing.data["color"] as? String { colorField.stringValue = color }
                if let brightness = thing.data["brightness"] as? Double { brightnessField.doubleValue = brightness }
                
                thingXField.doubleValue = Double(thing.position.x)
                thingYField.doubleValue = Double(thing.position.y)
                
                compass.selectedThing = thing
                compass.dataChanged()
            } else if let device = selectedFlare as? Device {
                NSLog("Selected \(device)")
                defaults.setObject(newFlare.id, forKey: "selectedDeviceId")
                tabView.selectTabViewItemAtIndex(3)
                mapDirectionButtons.hidden = false
                compassDirectionButtons.hidden = false

                if let angle = device.data["angle"] as? String { angleField.stringValue = angle }
                deviceXField.doubleValue = Double(device.position.x)
                deviceYField.doubleValue = Double(device.position.y)
                
                if let environment = flareManager.environmentForFlare(device) {
                    compass.environment = environment
                    compass.device = device
                    compass.dataChanged()
                }
            }
        }
        
        map.dataChanged()
    }
}
