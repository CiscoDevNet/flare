# Introduction



# Getting Started



# Developing with Swift

### Download

### Setup

The sample code for OS X, iOS and watchOS are distributed in one archive. To get started, open the Flare.xcworkspace to view all the projects in one window. 

The SocketIO framework is written in Swift and works on OS X, iOS and watchOS. 

The Flare framework is written in Swift, and works on OS X, iOS and watchOS. Beacon localization services depend on Bluetooth hardware and work only on iOS. The Swift framework depends on the SocketIO framework. 

### Mac

The Explorer app runs on OS X and provides an overview of all environments, zones, things and devices in the database. It depends on both the SocketIO and the Flare frameworks. 

To build for Mac, first build the SocketIO framework, then build the Flare framework, then build the Explorer app. When building the frameworks, make sure to choose Edit Scheme and change the build configuration from Debug to Release. The built frameworks will be placed in the Output/Release/ folder inside the folder for each project, where other projects link to them. 

By default, the app connects to the server on localhost at port 1234. You can specify another server or port in the Preferences. 

### iOS

To build for iOS, first build the SocketIO framework, then the Flare framework, then the Trilateral app. 

The iOS frameworks have a build script that can produce a framework for both devices and the iOS Simulator. When building the frameworks, make sure to choose Edit Scheme and change the build configuration from Debug to Release. It is necessary to build once for the iOS Simualtor (by selecting any device model) and then once again for devices (by selecting either "Device" or the name of an attached device). Once you have done so, the build script will produce a universal framework. The built frameworks will be placed in the Output/Release-iphoneuniversal/ folder inside the folder for each project, where other projects link to them. You can verify that the framework has architectures for both the simulator (i386 and x86_64) and devices (armv7 and arm64) like this:

	$ cd /path/to/Flare 
	$ cd Output/Release-iphoneuniversal/Flare.framework/
	$ file Flare
	Flare: Mach-O universal binary with 4 architectures
	Flare (for architecture i386):	Mach-O dynamically linked shared library i386
	Flare (for architecture x86_64):	Mach-O 64-bit dynamically linked shared library x86_64
	Flare (for architecture armv7):	Mach-O dynamically linked shared library arm
	Flare (for architecture arm64):	Mach-O 64-bit dynamically linked shared library

By default, the app connects to the server on localhost at port 1234. That works fine in the simulator, but on a device you'll need to specify the server and port in the Settings app.

### Framework documentation 

The Flare framework contains several classes:

FlareManager is a high level class with methods for calling both the Flare REST API (for describing objects in the environment) and the Flare Socket.IO API (for realtime communication between objects). 

FlareModel.swift contains definitions of the Environment, Zone, Thing and Device classes, which all inherit common variables and methods from the abstract Flare superclass. These objects can be initialized with JSON objects returned by the FlareManager. 

APIManager is a class for calling a generic REST API. Its main method is sendRequest(....), and has several convenience methods with fewer arguments. It makes HTTP requests asynchronously, and calls the handler closure when the response is available. For example, to send a request like this ###, you can call ###. Note that when calling a method in Swift where the last argument is a closure, you can specify the closure _outside_ the parentheses, and the argument types can be inferred. 

BeaconManager is a class allows an iOS device to calculate its position in the environment based on the distance to three or more iBeacons. 

Extensions.swift contains a number of useful extensions, which allow you to do things like subtract one CGPoint from another to find the diagonal distance. Swift is a highly extensible language, and many operators are overridden on particular pairs of types. 

### Using FlareManager

You can use the FlareManager to calling both the Flare REST API (for describing objects in the environment) and the Flare Socket.IO API (for realtime communication between objects). 

Provide the host and port when initializing the FlareManager:

	var flareManager = FlareManager(host, port)
	
Set the delegate to receive SocketIO message callbacks:
	
	flareManager.delegate = self

Connect to the server before calling the SocketIO interface:

	flareManager.connect()
	
To call a REST method:

	flareManager.getEnvironments() {jsonArray in ... }
	
To call a SocketIO method:

	flareManager.getData(environment)
	
Implement a delegate method to receive SocketIO message callbacks:

	func didReceiveData(flare: Flare, data: JSONDictionary) {
        NSLog("\(flare.name) data: \(data)")
    }
	
For more examples, see the [SocketIO using Swift](tutorials/socketio-swift.html) tutorial.

### Using BeaconManager

You can use the BeaconManager to determine the location of the user's device based on the distances from the beacons in the environment.

Create a BeaconManager:

	var beaconManager = BeaconManager()
	
Set the delegate to receive callbacks:
	
	beaconManager.delegate = self

Load the environment into the BeaconManager:
        
    beaconManager.loadEnvironment(environment)

Start scanning for beacons:

	self.beaconManager.start()

Implement a delegate method to be notified when the device's position changes:

    func devicePositionDidChange(position: CGPoint) {
        device!.position = position
        flareManager.setPosition(device!, position: position)
    }

For a complete example, see the [Beacons](beacons.html) tutorial.
