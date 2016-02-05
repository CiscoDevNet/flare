//
//  NearbyThingController.swift
//  Trilateral
//
//  Created by Andrew Zamler-Carhart on 12/9/15.
//  Copyright © 2015 Cisco. All rights reserved.
//

import UIKit
import Flare

class CatalogController: UITableViewController, FlareController {
    
    var appDelegate = UIApplication.sharedApplication().delegate as! AppDelegate
    let thingCellIdentifier = "ThingCell"
    
    var currentEnvironment: Environment? { didSet(value) {
        self.tableView.reloadData()
        }}
    var currentZone: Zone?
    var device: Device?
    var nearbyThing: Thing? { didSet(value) {
            // update selection
            dataChanged()
        }}
    
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        self.tableView.contentInset = UIEdgeInsetsMake(20, 0, 0, 0)
        appDelegate.flareController = self
        appDelegate.updateFlareController()
        
        dataChanged()
    }
    
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        if currentEnvironment == nil { return 0 }
        return currentEnvironment!.zones.count
    }
    
    override func tableView(tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if currentEnvironment == nil { return "" }
        return currentEnvironment!.zones[section].name
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return currentEnvironment!.zones[section].things.count
    }

    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier(thingCellIdentifier) as! ThingCell
        cell.device = device
        cell.thing = currentEnvironment!.zones[indexPath.section].things[indexPath.row]
        return cell
    }
    
    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        if currentEnvironment != nil {
            let thing = currentEnvironment!.zones[indexPath.section].things[indexPath.row]
            appDelegate.nearbyThing = thing
        }
    }
    
    func dataChanged() {
        if currentEnvironment != nil {
            for zone in currentEnvironment!.zones {
                zone.things.sortInPlace({
                    return device!.distanceTo($0) < device!.distanceTo($1)
                })
            }

            self.tableView.reloadData()
            
            for (section, zone) in currentEnvironment!.zones.enumerate() {
                for (row, thing) in zone.things.enumerate() {
                    if thing == nearbyThing {
                        self.tableView.selectRowAtIndexPath(NSIndexPath(forRow: row, inSection: section), animated: false, scrollPosition: .None)
                    }
                }
            }
        }
    }
    
    func animate() {
        // self.tableView.reloadData()
    }
}