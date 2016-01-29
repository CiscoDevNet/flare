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
    
    @IBOutlet weak var imageView: UIImageView!
    @IBOutlet weak var nearbyThingLabel: UILabel!
    @IBOutlet weak var nearbyThingComment: UILabel!
    // @IBOutlet weak var colorLabel: UILabel!
    // @IBOutlet weak var brightnessLabel: UILabel!
    @IBOutlet weak var slider: UISlider!
    @IBOutlet weak var powerSwitch: UISwitch!
    
    var colorButtons = [Int:ColorButton]()
    @IBOutlet weak var colorButton1: ColorButton!
    @IBOutlet weak var colorButton2: ColorButton!
    @IBOutlet weak var colorButton3: ColorButton!
    @IBOutlet weak var colorButton4: ColorButton!
    @IBOutlet weak var colorButton5: ColorButton!
    @IBOutlet weak var colorButton6: ColorButton!
    
    var currentEnvironment: Environment?
    var currentZone: Zone?
    var device: Device?
    var nearbyThing: Thing? { didSet(value) {
        updateColors()
        dataChanged()
    }}
    
    var colors = [String]()
    var defaultColors = ["red", "orange", "yellow", "green", "blue", "purple"]

    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        appDelegate.flareController = self
        appDelegate.updateFlareController()

        colorButtons[0] = colorButton1
        colorButtons[1] = colorButton2
        colorButtons[2] = colorButton3
        colorButtons[3] = colorButton4
        colorButtons[4] = colorButton5
        colorButtons[5] = colorButton6
        
        updateColors()
        dataChanged()
    }
    
    func updateColors() {
        if let options = nearbyThing?.data["options"] as? [String] {
            colors = options.count > 0 ? options : defaultColors;
        } else {
            colors = defaultColors;
        }
        
        for (index,colorButton) in colorButtons {
            if index < colors.count {
                colorButton.colorName = colors[index]
            } else {
                colorButton.colorName = "clear"
            }
            colorButton.setNeedsDisplay()
        }
    }
    
    func dataChanged() {
        nearbyThingLabel.text = nearbyThing?.name ?? "none"
        nearbyThingComment.text = nearbyThing?.comment ?? ""

        if let color = nearbyThing?.data["color"] as? String {
            // colorLabel.text = color
            
            for (index, colorButton) in colorButtons {
                colorButton.selected = index < colors.count && color == colors[index]
            }
            
            imageView.image = UIImage(named: "\(nearbyThing!.name.lowercaseString)-\(color)")
        } else {
            // colorLabel.text = ""
            
            for (_, colorButton) in colorButtons {
                colorButton.selected = false
            }
            
            imageView.image = nil
        }
        
        if let brightness = nearbyThing?.data["brightness"] as? Double {
            // brightnessLabel.text = "\(brightness)"
            slider.value = Float(brightness)
        } else {
            // brightnessLabel.text = ""
            slider.value = 0.5
        }
        
        if let on = nearbyThing?.data["on"] {
            powerSwitch.on = on as! NSObject == 1
        } else {
            powerSwitch.on = false
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
    
    @IBAction func setOn(sender: UISwitch) {
        appDelegate.setNearbyThingData("on", value: sender.on)
    }
}