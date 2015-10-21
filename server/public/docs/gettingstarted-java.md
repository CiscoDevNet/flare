## Developing with Java

### Download

### Setup

The sample code contains an Android Studio project with several modules. The flare module contains the FlareManager client, as well as classes for Environment, Zone, Thing and Device. 

### Apps


### Dependencies

The Flare library has a few dependencies:

- [volley](https://github.com/mcxiaoke/android-volley) is used for making HTTP calls
- [socket.io-client](https://github.com/socketio/socket.io-client-java) is used for making Socket.IO calls
- [android-beacon-library](https://altbeacon.github.io/android-beacon-library/) is used for discovering AltBeacons

You can include them in your build.gradle file:

	repositories {
	    mavenCentral()
	    jcenter()
	}
   
	dependencies {
	    compile 'com.mcxiaoke.volley:library:1.0.18'
	    compile 'com.github.nkzawa:socket.io-client:0.6.0'
		compile 'org.altbeacon:android-beacon-library:2+'
	}

The Flare library makes extensive use of [lambdas](http://www.oracle.com/webfolder/technetwork/tutorials/obe/java/Lambda-QuickStart/index.html), a feature of Java 8. This [tutorial](http://viralpatel.net/blogs/lambda-expressions-java-tutorial/) explains how lambdas work.

Unfortunately, Android does not yet (as of version 5.1) support Java 8 for application development. However, you can use the [gradle-retrolambda](https://github.com/evant/gradle-retrolambda) plugin to add support for the lambda syntax. You can add the following to your build.gradle file:

    buildscript {
        repositories {
            mavenCentral()
        }

	    dependencies {
            classpath 'me.tatarka:gradle-retrolambda:3.2.2'
        }
    }

    android {
        compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
		
        repositories {
            mavenCentral()
        }

        apply plugin: 'me.tatarka.retrolambda'
    }

### Library documentation 

The Flare library contains several classes:

FlareManager is a high level class with methods for calling both the Flare REST API (for describing objects in the environment) and the Flare Socket.IO API (for realtime communication between objects). 

The Environment, Zone, Thing and Device classes all inherit common variables and methods from the abstract Flare superclass. These objects can be initialized with JSON objects returned by the FlareManager. 

BeaconManager is a class allows an Android device to calculate its position in the environment based on the distance to three or more beacons. 

### Using FlareManager

You can use the FlareManager to calling both the Flare REST API (for describing objects in the environment) and the Flare Socket.IO API (for realtime communication between objects). 

Provide the host and port when initializing the FlareManager:

	var flareManager = new FlareManager(host, port);
	
Set the activity, which is used by Volley to create a request queue:
	
	flareManager.setActivity(this);

Set the delegate (which should implement the FlareManager.Delegate interface) to receive SocketIO message callbacks:
	
	flareManager.setDelegate(this);

Connect to the server before calling the SocketIO interface:

	flareManager.connect();
	
To call a REST method:

	flareManager.listEnvironments((jsonArray) -> {
		for (JSONObject json : jsonArray) {
			Environment environment = new Environment(json);
		}
	}
	
	flareManager.getDevice(deviceId, environment.getId(), (json) -> {
		Device device = new Device(json);
	});
	
These methods use lambdas for asynchronous requests. When the response has been received, the lambda is called with a JSONObject or an array of JSONObjects. 
					
To call a SocketIO method:

	flareManager.getData(environment);
	
Implement a delegate method to receive SocketIO message callbacks:

	public void didReceiveData(Flare flare, JSONObject data) {
		Log.d(TAG, flare.getName() + " data: " + data.toString());
	}
	
For more examples, see the [SocketIO using Java](tutorials/socketio-java.html) tutorial.

### Using BeaconManager

You can use the BeaconManager to determine the location of the user's device based on the distances from the beacons in the environment.

Create a BeaconManager:

	beaconManager = new BeaconManager();
	
Set the delegate to receive callbacks:
	
	beaconManager.setDelegate(this);

Load the environment into the BeaconManager:
        
    beaconManager.loadEnvironment(environment);

Start scanning for beacons:

	beaconManager.start();

Implement a delegate method to be notified when the device's position changes:

	... 

For a complete example, see the [Beacons](beacons.html) tutorial.
