//
//  InterfaceController.swift
//  Things WatchKit Extension
//
//  Created by Andrew Zamler-Carhart on 6/12/15.
//  Copyright © 2015 Cisco. All rights reserved.
//

import WatchKit
import WatchConnectivity
import Foundation


class DetailsController: WKInterfaceController, WCSessionDelegate {
    
    var session: WCSession?
    
    @IBOutlet weak var thingImage: WKInterfaceImage!
    @IBOutlet weak var descriptionLabel: WKInterfaceLabel!

    @IBOutlet weak var actionButton1: WKInterfaceButton!
    @IBOutlet weak var actionButton2: WKInterfaceButton!
    @IBOutlet weak var actionButton3: WKInterfaceButton!
    @IBOutlet weak var actionButton4: WKInterfaceButton!
    @IBOutlet weak var actionButton5: WKInterfaceButton!
    var actionButtons = [WKInterfaceButton]()
    
    var thing: Thing!
    
    override func awakeWithContext(context: AnyObject?) {
        actionButtons = [actionButton1, actionButton2, actionButton3, actionButton4, actionButton5]
        
        super.awakeWithContext(context)
        
        if WCSession.isSupported() {
            session = WCSession.defaultSession()
            session!.delegate = self
            session!.activateSession()
        }

        if let someThing = context as? Thing {
            self.thing = someThing
            setTitle(thing.name)
            
            // thingImage.setImageNamed(thing.id)
            descriptionLabel.setText(thing.comment)
            updateButtons()
        }
    }
    
    func updateButtons() {
        if let actions = thing.data["options"] as? [String] {
            for (index,button) in actionButtons.enumerate() {
                if index < actions.count {
                    button.setTitle(actions[index].titlecaseString())
                    button.setHidden(false)
                } else {
                    button.setHidden(true)
                }
            }
        } else { // there are no actions
            for button in actionButtons {
                button.setHidden(true)
            }
        }
    }
    
    @IBAction func performAction1() {
        performAction(0)
    }

    @IBAction func performAction2() {
        performAction(1)
    }
    
    @IBAction func performAction3() {
        performAction(2)
    }
    
    @IBAction func performAction4() {
        performAction(3)
    }
    
    @IBAction func performAction5() {
        performAction(4)
    }
    
    
    func performAction(index: Int) {
        if let actions = thing.data["options"] as? [String] {
            if index < actions.count {
                let action = actions[index]
                NSLog("Action: \(action)")
                
                session!.sendMessage(["data":["thing":thing.id, "key":"color", "value":action]],
                    replyHandler: { (message: [String : AnyObject]) -> Void in
                        NSLog("Action sent")
                    },
                    errorHandler: { (error: NSError) -> Void in
                        NSLog("Couldn't get location: \(error)")
                    }
                )
            }
        }
    }
    
    override func willActivate() {
        // This method is called when watch view controller is about to be visible to user
        super.willActivate()
    }
    
    override func didDeactivate() {
        // This method is called when watch view controller is no longer visible
        super.didDeactivate()
    }
    
}
