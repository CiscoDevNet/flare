//
//  ThingCell.swift
//  Trilateral
//
//  Created by Andrew Zamler-Carhart on 2/2/16.
//  Copyright © 2016 Cisco. All rights reserved.
//

import UIKit
import Flare

class ThingCell: UITableViewCell {
    var device: Device?
    var thing: Thing? { didSet(value) { update() }}

    @IBOutlet weak var thingImage: UIImageView!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var commentLabel: UILabel!
    @IBOutlet weak var priceLabel: UILabel!
    @IBOutlet weak var distanceLabel: UILabel!

    func update() {
        nameLabel.text = thing?.name
        commentLabel.text = thing?.comment
        
        if let price = thing?.data["price"] as? Int {
            priceLabel.text = "$\(price)"
        }
        
        if let imageName = thing?.imageName() {
            thingImage.image = UIImage(named: imageName)
        }
        
        if let distance = device?.distanceTo(thing!) {
            distanceLabel.text = String(format: "%.1fm", distance)
        } else {
            distanceLabel.text = ""
        }
    }
    
}

