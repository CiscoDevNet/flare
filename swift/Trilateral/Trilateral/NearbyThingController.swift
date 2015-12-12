//
//  NearbyThingController.swift
//  Trilateral
//
//  Created by Andrew Zamler-Carhart on 12/9/15.
//  Copyright © 2015 Cisco. All rights reserved.
//

import UIKit
import Flare

class NearbyThingController: UIViewController, FlareController {
    
    var appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    
    @IBOutlet weak var nearbyThingLabel: UILabel!
    @IBOutlet weak var nearbyThingComment: UILabel!
    @IBOutlet weak var colorLabel: UILabel!
    @IBOutlet weak var brightnessLabel: UILabel!
    @IBOutlet weak var slider: UISlider!
    
    var colorButtons = [String:ColorButton]()
    @IBOutlet weak var redButton: ColorButton!
    @IBOutlet weak var orangeButton: ColorButton!
    @IBOutlet weak var yellowButton: ColorButton!
    @IBOutlet weak var greenButton: ColorButton!
    @IBOutlet weak var blueButton: ColorButton!
    @IBOutlet weak var purpleButton: ColorButton!
    
    var currentEnvironment: Environment?
    var currentZone: Zone?
    var device: Device?
    var nearbyThing: Thing? { didSet(value) { dataChanged() }}
    
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        appDelegate.flareController = self
        appDelegate.updateFlareController()

        colorButtons["red"] = redButton
        colorButtons["orange"] = orangeButton
        colorButtons["yellow"] = yellowButton
        colorButtons["green"] = greenButton
        colorButtons["blue"] = blueButton
        colorButtons["purple"] = purpleButton
        for (colorName, colorButton) in colorButtons {
            colorButton.colorName = colorName
            colorButton.setNeedsDisplay()
        }
        
        self.dataChanged()
    }
    
    func dataChanged() {
        nearbyThingLabel.text = nearbyThing?.name ?? "none"
        nearbyThingComment.text = nearbyThing?.comment ?? ""

        if let color = nearbyThing?.data["color"] as? String {
            colorLabel.text = color
            
            for (colorName, colorButton) in colorButtons {
                colorButton.selected = color == colorName
            }
        } else {
            colorLabel.text = ""
            
            for (_, colorButton) in colorButtons {
                colorButton.selected = false
            }
        }
        
        if let brightness = nearbyThing?.data["brightness"] as? Double {
            brightnessLabel.text = "\(brightness)"
            slider.value = Float(brightness)
        } else {
            brightnessLabel.text = ""
            slider.value = 0.5
        }
    }
    
    @IBAction func performAction(sender: UIButton) {
        let identifiers = sender.accessibilityIdentifier!.componentsSeparatedByString(" ")
        let action = identifiers.first!
        
        if nearbyThing != nil {
            appDelegate.flareManager.performAction(nearbyThing!, action: action, sender: device)
        }
    }
    
    @IBAction func setColor(sender: ColorButton) {
        appDelegate.setNearbyThingData("color", value: sender.colorName)
    }

    @IBAction func setBrightness(sender: UISlider) {
        let brightness = Double(slider.value).roundTo(0.1)
        appDelegate.setNearbyThingData("brightness", value: brightness)
    }
}