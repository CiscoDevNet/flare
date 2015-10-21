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
    @IBOutlet weak var outlineView: NSOutlineView!
    @IBOutlet weak var tabView: NSTabView!
    @IBOutlet weak var map: IndoorMap!
    
    @IBOutlet weak var idField: NSTextField!
    @IBOutlet weak var nameField: NSTextField!
    @IBOutlet weak var commentField: NSTextField!

    @IBOutlet weak var uuidField: NSTextField!
    @IBOutlet weak var environmentXField: NSTextField!
    @IBOutlet weak var environmentYField: NSTextField!
    @IBOutlet weak var environmentWidthField: NSTextField!
    @IBOutlet weak var environmentHeightField: NSTextField!

    @IBOutlet weak var majorField: NSTextField!
    @IBOutlet weak var zoneXField: NSTextField!
    @IBOutlet weak var zoneYField: NSTextField!
    @IBOutlet weak var zoneWidthField: NSTextField!
    @IBOutlet weak var zoneHeightField: NSTextField!

    @IBOutlet weak var minorField: NSTextField!
    @IBOutlet weak var colorField: NSTextField!
    @IBOutlet weak var brightnessField: NSTextField!
    @IBOutlet weak var xField: NSTextField!
    @IBOutlet weak var yField: NSTextField!

    @IBOutlet weak var angleField: NSTextField!
    @IBOutlet weak var deviceXField: NSTextField!
    @IBOutlet weak var deviceYField: NSTextField!

    @IBOutlet weak var nearbyView: NSView!
    @IBOutlet weak var nearbyIdField: NSTextField!
    @IBOutlet weak var nearbyNameField: NSTextField!
    @IBOutlet weak var nearbyCommentField: NSTextField!
    @IBOutlet weak var nearbyDistanceField: NSTextField!
    @IBOutlet weak var nearbyAngleField: NSTextField!
    
    var flareManager = FlareManager(host: "localhost", port: 1234)
    var environments = [Environment]()
    var selectedFlare, nearbyFlare: Flare?
    var defaults: NSUserDefaults
    
    override init() {
        defaults = NSUserDefaults.standardUserDefaults()
        
        super.init()
    }
    
    func applicationDidFinishLaunching(aNotification: NSNotification) {
        let path = NSBundle.mainBundle().pathForResource("Defaults", ofType: "plist")
        let factorySettings = NSDictionary(contentsOfFile: path!)
        let defaultsController = NSUserDefaultsController.sharedUserDefaultsController()
        
        defaults.registerDefaults(factorySettings! as! [String : AnyObject])

        defaultsController.addObserver(self, forKeyPath: "values.host", options: nil, context: nil)
        defaultsController.addObserver(self, forKeyPath: "values.port", options: nil, context: nil)

        load()
    }
    
    func load() {
        var host = defaults.stringForKey("host")!
        var port = defaults.integerForKey("port")
        
        flareManager = FlareManager(host: host, port: port)
        flareManager.delegate = self
        flareManager.debugHttp = false
        flareManager.debugSocket = true
        
        NSLog("Flare server: \(flareManager.server)")
        
        flareManager.connect()
        
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
        change: [NSObject : AnyObject]?, context: UnsafeMutablePointer<Void>)
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
        var selected = sender.selectedSegment

        switch selected {
        case 0: newFlare(sender)
        case 1: deleteFlare(sender)
        default: break
        }
    }
    
    @IBAction func newFlare(sender: AnyObject) {
        var template: JSONDictionary = ["name":"Untitled", "description":"", "data":[:]]

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
                nearbyAngleField.doubleValue = angle
            }
        }
    }
    
    func didReceivePosition(flare: Flare, position: CGPoint, sender: Flare?) {
        NSLog("\(flare.name) position: \(position)")
        
        if flare == selectedFlare {
            if flare is Thing {
                xField.doubleValue = Double(position.x)
                yField.doubleValue = Double(position.y)
            } else if flare is Device {
                deviceXField.doubleValue = Double(position.x)
                deviceYField.doubleValue = Double(position.y)
            }
        } else if flare == nearbyFlare {
            if let device = nearbyFlare as? Device, thing = selectedFlare as? Thing {
                var distance = device.position - thing.position
                nearbyDistanceField.doubleValue = distance
            }
        }
        
        map.dataChanged()
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
        
        if thing == selectedFlare && nearbyFlare != device {
            nearbyFlare = device
            
            flareManager.subscribe(device)
            flareManager.getData(device)
            flareManager.getPosition(device)
            
            nearbyIdField.stringValue = device.id
            nearbyNameField.stringValue = device.name
            nearbyCommentField.stringValue = device.comment
            nearbyDistanceField.doubleValue = distance
            if let angle = device.data["angle"] as? Double {
                nearbyAngleField.doubleValue = angle
            }
            
            nearbyView.hidden = false
        }
    }
    
    func far(thing: Thing, device: Device) {
        NSLog("\(device.name) far: \(thing.name)")
        
        if thing == selectedFlare && device == nearbyFlare {
            nearbyView.hidden = true
            
            flareManager.unsubscribe(device)
            
            nearbyFlare = nil
        }
    }
    
    // MARK: Actions
    
    @IBAction func changeName(sender: NSTextField) {
        if let flare = selectedFlare {
            var name = sender.stringValue
            flare.name = name
            self.outlineView.reloadData()
            flareManager.updateFlare(flare, json: ["name":name]) {json in }
        }
    }
    
    @IBAction func changeComment(sender: NSTextField) {
        if let flare = selectedFlare {
            var comment = sender.stringValue
            flare.comment = comment
            flareManager.updateFlare(flare, json: ["description":comment]) {json in }
        }
    }
    
    @IBAction func changeDataString(sender: NSTextField) {
        if let flare = selectedFlare {
            var value = sender.stringValue
            flare.data[sender.identifier!] = value
            flareManager.setData(flare, key: sender.identifier!, value: sender.stringValue, sender: nil)
        }
    }
    
    @IBAction func changeDataInteger(sender: NSTextField) {
        if let flare = selectedFlare {
            var value = sender.integerValue
            flare.data[sender.identifier!] = value
            flareManager.setData(flare, key: sender.identifier!, value: sender.stringValue, sender: nil)
        }
    }
    
    @IBAction func changeDataDouble(sender: NSTextField) {
        if let flare = selectedFlare {
            var value = sender.doubleValue
            flare.data[sender.identifier!] = value
            flareManager.setData(flare, key: sender.identifier!, value: sender.stringValue, sender: nil)
        }
    }

    @IBAction func changePosition(sender: NSTextField) {
        if let thing = selectedFlare as? Thing {
            var position = CGPoint(x: xField.doubleValue, y: yField.doubleValue)
            thing.position = position
            flareManager.setPosition(thing, position: position, sender: nil)
        } else if let device = selectedFlare as? Device {
            var position = CGPoint(x: deviceXField.doubleValue, y: deviceYField.doubleValue)
            device.position = position
            flareManager.setPosition(device, position: position, sender: nil)
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
    
    @IBAction func changeAngle(sender: NSTextField) {
        if let flare = nearbyFlare {
            var value = sender.doubleValue
            flare.data["angle"] = value
            flareManager.setData(flare, key: "angle", value: value, sender: nil)
        }
    }
    
    @IBAction func performAction(sender: NSButton) {
        if let flare = selectedFlare, action = sender.identifier {
            flareManager.performAction(flare, action: action, sender: nil)
        }
    }
    
    @IBAction func nearbyPerformAction(sender: NSButton) {
        if let flare = nearbyFlare, action = sender.identifier {
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
        var view = (item as! Flare).id == "" ?
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
            majorField.stringValue = ""
            zoneXField.stringValue = ""
            zoneYField.stringValue = ""
            zoneWidthField.stringValue = ""
            zoneHeightField.stringValue = ""
            minorField.stringValue = ""
            colorField.stringValue = ""
            brightnessField.stringValue = ""
            xField.stringValue = ""
            yField.stringValue = ""
            angleField.stringValue = ""
            deviceXField.stringValue = ""
            deviceYField.stringValue = ""
        }
        
        selectedFlare = selectedItem()
        nearbyView.hidden = true
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
            }

            if let environment = selectedFlare as? Environment {
                tabView.selectTabViewItemAtIndex(0)

                if let uuid = environment.data["uuid"] as? String { uuidField.stringValue = uuid }
                environmentXField.doubleValue = Double(environment.perimeter.origin.x)
                environmentYField.doubleValue = Double(environment.perimeter.origin.y)
                environmentWidthField.doubleValue = Double(environment.perimeter.size.width)
                environmentHeightField.doubleValue = Double(environment.perimeter.size.height)
            } else if let zone = selectedFlare as? Zone {
                NSLog("Selected \(zone)")
                tabView.selectTabViewItemAtIndex(1)
                
                if let major = zone.data["major"] as? String { majorField.stringValue = major }
                zoneXField.doubleValue = Double(zone.perimeter.origin.x)
                zoneYField.doubleValue = Double(zone.perimeter.origin.y)
                zoneWidthField.doubleValue = Double(zone.perimeter.size.width)
                zoneHeightField.doubleValue = Double(zone.perimeter.size.height)
            } else if let thing = selectedFlare as? Thing {
                NSLog("Selected \(thing)")
                tabView.selectTabViewItemAtIndex(2)
                
                if thing.minor != nil { minorField.integerValue = thing.minor! }

                flareManager.getPosition(thing)
                
                if let color = thing.data["color"] as? String { colorField.stringValue = color }
                if let brightness = thing.data["brightness"] as? Double { brightnessField.doubleValue = brightness }
                
                xField.doubleValue = Double(thing.position.x)
                yField.doubleValue = Double(thing.position.y)
            } else if let device = selectedFlare as? Device {
                NSLog("Selected \(device)")
                tabView.selectTabViewItemAtIndex(3)
                
                if let angle = device.data["angle"] as? String { angleField.stringValue = angle }
                deviceXField.doubleValue = Double(device.position.x)
                deviceYField.doubleValue = Double(device.position.y)
            }
        }
    }
}

