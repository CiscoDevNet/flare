//
//  DeviceController.swift
//  Trilateral
//
//  Created by Andrew Zamler-Carhart on 12/7/15.
//  Copyright © 2015 Cisco. All rights reserved.
//

import UIKit
import Flare

class DeviceController: UIViewController, FlareController {

    var appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate

    @IBOutlet weak var environmentLabel: UILabel!
    @IBOutlet weak var environmentComment: UILabel!
    @IBOutlet weak var zoneLabel: UILabel!
    @IBOutlet weak var zoneComment: UILabel!
    @IBOutlet weak var deviceLabel: UILabel!
    @IBOutlet weak var deviceComment: UILabel!
    @IBOutlet weak var positionLabel: UILabel!
    @IBOutlet weak var angleLabel: UILabel!
    @IBOutlet weak var nearbyThingLabel: UILabel!
    @IBOutlet weak var nearbyThingComment: UILabel!
    
    var currentEnvironment: Environment? { didSet { dataChanged() }}
    var currentZone: Zone? { didSet(value) { dataChanged() }}
    var device: Device? { didSet { dataChanged() }}
    var nearbyThing: Thing? { didSet(value) { dataChanged() }}
    
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        appDelegate.flareController = self
        appDelegate.updateFlareController()
        self.dataChanged()
        self.animate()
    }

    func dataChanged() {
        environmentLabel.text = currentEnvironment?.name ?? "none"
        environmentComment.text = currentEnvironment?.comment ?? ""
        zoneLabel.text = currentZone?.name ?? "none"
        zoneComment.text = currentZone?.comment ?? ""
        deviceLabel.text = device?.name ?? "none"
        deviceComment.text = device?.comment ?? ""
        nearbyThingLabel.text = nearbyThing?.name ?? "none"
        nearbyThingComment.text = nearbyThing?.comment ?? ""
        
        if let position = device?.position {
            positionLabel.text = String(format: "%.2f, %.2f", position.x.roundTo(0.05), position.y.roundTo(0.05))
        } else {
            positionLabel.text = "0.00, 0.00"
        }

        if let angle = device?.data["angle"] as? Double {
            angleLabel.text = "\(Int(angle))°"
        } else {
            angleLabel.text = "0°"
        }
    }
    
    func animate() {

    }
}