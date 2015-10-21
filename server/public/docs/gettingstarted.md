## Getting started


### Server

The Flare API server in implemented in [Node.js](https://nodejs.org) using JavaScript. It uses the [MongoDB](https://www.mongodb.org) database.

The source code is available as a download, so you can run it yourself. 

You have several options for running the server:

- To learn how it works, you can connect to a shared instance in the DevNet Sandbox. 
- For development, you can easily run the server on your own computer. 
- For deployment on a local network, you can run the server on a local server.
- For a large-scale deployment, you can run the server in the cloud. 

See the [Readme](readme.html) file for instructions on downloading the necessary software and running the server. Regardless of where you choose to install it, you have the option of installing it directly or running it in a virtual machine using Vagrant. 

### Documentation

The Flare API server serves its own documentation. For example, if you have the server running on localhost using port 1234, you can point your browser to [http://localhost:1234](http://localhost:1234) to see an outline of all documentation and other resources. 

While you can browse the documentation on DevNet or by opening the HTML files directly in your browser, note that some of the documentation is dynamic and needs to be able to connect to the server to work properly. For example, when you use the [REST API Console](../api-console/index.html?raml=/docs/flare-rest.raml) and the [Socket.IO API console](../console.html), you can call APIs and see the results in realtime. 

### Data

To get started with your own instance of the server, you'll need to add some data to the database by creating some environment, zone, thing, and device objects. You can do this in several ways.

- You can load some sample data from a JSON file. See the file model.json for an example of the data format. To load the data into the database, you can use this command:

	node import.js model.json
	
- If you have exported some data from a previous installation using MongoDB, you can use [mongoimport](http://www.mkyong.com/mongodb/mongodb-import-and-export-example/).

- You can call the API directly using curl or [poster](https://github.com/dengzhp/chrome-poster). For example, here's a command to create a device:

	curl --data "{\"data\":{\"color\":\"red\"},\"description\": \"iPhone 6 (32 GB)\",\"name\": \"My iPhone\",\"position\": {\"x\": 3,\"y\": 1}}" -H "Content-Type: application/json" http://localhost:1234/devices

- You can use the [REST API Console](../api-console/index.html?raml=/docs/flare-rest.raml) to create objects using POST requests. 
	
- You can use the Explorer app for Mac, which is part of the sample code, to create objects using a graphical interface. 
	
- You can write a script to get data out of your own enterprise database and add it to Flare. 

### Location technologies

Flare currently supports two kinds of indoor location technologies, beacons and CMX. 

Beacons are tiny devices that transmit a signal over Bluetooth at regular intervals. A device can measure the signal strength of three or more beacons at known locations to determine its own location. The device can then send its location to the Flare server, which can then push the location to other things. 

The Flare sample code for iOS uses Apple's [iBeacon](https://developer.apple.com/ibeacon/) standard, while the sample code for Android uses [Radius Networks](http://www.radiusnetworks.com)' [AltBeacon](http://altbeacon.org) standard. We recommend beacons from Radius Networks as they are compatible with both standards. See the [beacons](beacons.html) documentation for more information on purchasing and setting up hardware.

[CMX](Connected Mobile Experience) is a Cisco platform that uses Wi-Fi access points to detect the location of user devices connected to the network. The location information can be transmitted from the CMX server to the Flare server, and then pushed to devices and things.

A CMX installation requires a controller module and three or more access points. See the [CMX](cmx.html) documentation for more information on purchasing and setting up hardware. 

### Clients

Several kinds of clients can talk to the Flare server. 

Users can run apps on their personal devices, including tablets, mobile phones and smart watches. The Flare server can track the location of these devices for enhanced interactions as users move around in a space. 

Things can run web or native apps that drive interactive displays. For example, when a user walks up to a display, it could greet them with a message and then allow them to control the display from their device.

Connected things can be controlled by small scripts that connect to the thing over a proprietary interface. 

Administrators can run web and native apps for creating, browsing and updating data on the server. 

Data can be imported into Flare from an enterprise database, either on a one-time or ongoing basis, using an import script written in the language of your choice. 

Monitoring scripts can subscribe to receive notifications for logging and analytics purposes. 

### Sample Code

Sample code is available for the following platforms:

- Web apps using JavaScript
- Mac, iOS and watchOS apps using Swift
- Android apps using Java

