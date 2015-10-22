//
//  AppDelegate.swift
//  ReflektorRemote
//
//  Created by Andrew Zamler-Carhart on 9/8/15.
//  Copyright (c) 2015 Cisco. All rights reserved.
//

import Cocoa
import Flare

@NSApplicationMain
class AppDelegate: NSObject, NSApplicationDelegate, FlareManagerDelegate {

    @IBOutlet weak var window: NSWindow!
    @IBOutlet weak var pageField: NSTextField!
    @IBOutlet weak var productPopup: NSPopUpButton!
    @IBOutlet weak var styleSlider: NSSlider!
    
    var flareManager = FlareManager(host: "localhost", port: 1234)
    var environmentId = "55e05d6032daa32ecbd30b5c"
    var zoneId = "55e05d6032daa32ecbd30b62"
    var reflektorId = "55ef20a8ffa743581980bb66"
    var reflektor: Thing?
    let products = ["coombe", "arholma", "dagarn", "erska", "kallax", "karlstad", "knislinge", "knopparp"]

    func applicationDidFinishLaunching(aNotification: NSNotification) {
        flareManager.delegate = self
        flareManager.debugHttp = true
        flareManager.debugSocket = true
        flareManager.connect()
        flareManager.getThing(reflektorId, environmentId: environmentId, zoneId: zoneId) { json in
            self.reflektor = Thing(json: json)
            self.flareManager.addToIndex(self.reflektor!)
            self.flareManager.subscribe(self.reflektor!)
            self.flareManager.getData(self.reflektor!)
        }
    }

    @IBAction func setPage(sender: NSTextField) {
        if reflektor != nil {
            flareManager.setData(reflektor!, key: "page", value: sender.integerValue, sender: nil)
        }
    }

    @IBAction func nextPrev(sender: NSSegmentedControl) {
        if reflektor != nil {
            switch sender.selectedSegment {
            case 0: flareManager.performAction(reflektor!, action: "prev", sender: nil)
            case 1: flareManager.performAction(reflektor!, action: "next", sender: nil)
            default: break
            }
        }
    }
    
    @IBAction func setProduct(sender: NSPopUpButton) {
        if reflektor != nil {
            if let product = sender.selectedItem?.title.lowercaseString {
                flareManager.setData(reflektor!, key: "product", value: product, sender: nil)
            }
        }
    }
    
    @IBAction func setStyle(sender: NSSlider) {
        if reflektor != nil {
            flareManager.setData(reflektor!, key: "index", value: sender.integerValue, sender: nil)
        }
    }

    func didReceiveData(flare: Flare, data: JSONDictionary, sender: Flare?) {
        if flare == reflektor {
            if let page = data["page"] as? Int {
                pageField.integerValue = page
            }
            
            if let product = data["product"] as? String, productIndex = products.indexOf(product) {
                productPopup.selectItemAtIndex(productIndex)
            }
            
            if let index = data["index"] as? Int {
                styleSlider.integerValue = index
            }
        }
    }

    func applicationWillTerminate(aNotification: NSNotification) {
        // Insert code here to tear down your application
    }
    
}

