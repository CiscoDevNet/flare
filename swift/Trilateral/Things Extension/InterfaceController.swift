//
//  InterfaceController.swift
//  Things Extension
//
//  Created by Andrew Zamler-Carhart on 2/7/16.
//  Copyright © 2016 Cisco. All rights reserved.
//

import WatchKit
import WatchConnectivity
import Foundation

class InterfaceController: WKInterfaceController, WCSessionDelegate {

    @IBOutlet weak var thingsTable: WKInterfaceTable!

    var things = [Thing]()
    var currentPosition = CGPoint(x: 0, y: 0)
    let numberFormatter = NSNumberFormatter()
    var defaults = NSUserDefaults.standardUserDefaults()
    var session: WCSession?

    override func awakeWithContext(context: AnyObject?) {
        super.awakeWithContext(context)
        
        numberFormatter.numberStyle = NSNumberFormatterStyle.DecimalStyle
        numberFormatter.maximumFractionDigits = 0
        
        if WCSession.isSupported() {
            session = WCSession.defaultSession()
            session!.delegate = self
            session!.activateSession()
        }
        
        for i in 1...3 {
            var thingInfo = JSONDictionary()
            thingInfo["name"] = "Thing \(i)"
            thingInfo["description"] = "Stuff \(i)"
            thingInfo["data"] = ["price":i * 10]
            thingInfo["position"] = ["x":i, "y":0]
            NSLog("Thing: \(thingInfo)")
            things.append(Thing(json: thingInfo))
        }
        
        delay(1.0) {
            NSLog("Getting things...")
            self.getThings()
            self.getPosition()
        }

        reloadTable()
    }

    override func willActivate() {
        // This method is called when watch view controller is about to be visible to user
        super.willActivate()
    }

    override func didDeactivate() {
        // This method is called when watch view controller is no longer visible
        super.didDeactivate()
    }
    
    func reloadTable() {
        if thingsTable.numberOfRows != things.count || things.count == 0 {
            thingsTable.setNumberOfRows(things.count, withRowType: "ThingRow")
        }
        
        // set the distance to each flare object from the current position
        for (_, flare) in things.enumerate() {
            flare.setDistanceFrom(currentPosition)
        }
        
        // sort the flare objects by distance
        things.sortInPlace { (one: Flare, two: Flare) -> Bool in
            return one.distance < two.distance
        }
        
        // update the name and distance of the table rows
        for (index, flare) in things.enumerate() {
            if let row = thingsTable.rowControllerAtIndex(index) as? TableRow {
                row.nameLabel.setText(flare.name)
                row.commentsLabel.setText(flare.comment)
                row.distanceLabel.setText(String(format:"%.1f", distanceString(flare)))
                if let price = flare.data["price"] as? Int {
                    row.priceLabel.setText("$\(price)")
                }
            }
        }
    }
    
    // returns a human readable string that represents the distance to the flare object
    func distanceString(flare: Flare) -> String {
        if let distance = flare.distance {
            return "\(distance)"
        } else {
            return ""
        }
    }
    
    override func contextForSegueWithIdentifier(segueIdentifier: String,
        inTable table: WKInterfaceTable, rowIndex: Int) -> AnyObject? {
            if segueIdentifier == "ShowDetails" {
                return things[rowIndex]
            }
            
            return nil
    }

    func getThings() {
        session!.sendMessage(["get":"things"],
            replyHandler: { (message: [String : AnyObject]) -> Void in
                self.gotThings(message)
            },
            errorHandler: { (error: NSError) -> Void in
                NSLog("Couldn't get things: \(error)")
        })
    }
    
    func gotThings(message: JSONDictionary) {
        self.things = Thing.loadJson(message)
        NSLog("Got \(self.things.count) things.")
        
        if (self.things.count > 0) {
            self.getPosition()
            self.reloadTable()
        }
    }
    
    // initializes the location from the defaults,
    // sends a message to the iPhone to ask for the location
    // and then sets the location (if it receives a reply)
    func getPosition() {
        let x = defaults.doubleForKey("x")
        let y = defaults.doubleForKey("y")
        if (x != 0.0) && (y != 0.0) {
            currentPosition = CGPoint(x:x, y:y)
        }
        
        if (session != nil) {
            session!.sendMessage(["get":"position"],
                replyHandler: { (message: [String : AnyObject]) -> Void in
                    self.setPosition(message)
                },
                errorHandler: { (error: NSError) -> Void in
                    NSLog("Couldn't get location: \(error)")
            })
        }
    }
    
    // parses the location message, saves the values to the defaults,
    // sets the location and reloads the table
    func setPosition(position: JSONDictionary) {
        if let x = position["x"] as? Double,
            y = position["y"] as? Double
        {
            defaults.setDouble(x, forKey: "x")
            defaults.setDouble(y, forKey: "y")
            
            currentPosition = CGPoint(x:x, y:y)
            reloadTable()
        }
    }
    
    func session(session: WCSession, didReceiveMessage message: [String : AnyObject]) {
        NSLog("Got message: \(message)")
        
        if let position = message["position"] as? JSONDictionary {
            setPosition(position)
        } else if let _ = message["things"] as? JSONArray {
            gotThings(message)
        }
    }
    
    func session(session: WCSession, didReceiveMessage message: [String : AnyObject],
        replyHandler: ([String : AnyObject]) -> Void) {
            NSLog("Got message (reply handler): \(message)")
            
            if let position = message["position"] as? JSONDictionary {
                setPosition(position)
            } else if let _ = message["things"] as? JSONArray {
                gotThings(message)
            }
    }

}
