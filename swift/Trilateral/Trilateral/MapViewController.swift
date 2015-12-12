//
//  ViewController.swift
//  Trilateral
//
//  Created by Andrew Zamler-Carhart on 3/24/15.
//  Copyright (c) 2015 Cisco. All rights reserved.
//

import UIKit
import Flare
import CoreLocation

class MapViewController: UIViewController {

    @IBOutlet weak var indoorMap: IndoorMap!
    
    var appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    
    override func viewDidAppear(animated: Bool) {
        appDelegate.flareController = indoorMap
        appDelegate.updateFlareController()
    }
    
    override func viewDidDisappear(animated: Bool) {

    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }


}
