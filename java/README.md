# Java

The Java sample code contains an [Android Studio](http://developer.android.com/tools/studio/) project with several modules. The flare module contains the FlareManager client, as well as classes for Environment, Zone, Thing and Device. 

# Dependencies

```all
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile 'com.mcxiaoke.volley:library:1.0.18'
    compile 'com.github.nkzawa:socket.io-client:0.6.0'
	compile 'org.altbeacon:android-beacon-library:2+'
}
```
The Flare library has a few dependencies:

- [volley](https://github.com/mcxiaoke/android-volley) is used for making HTTP calls
- [socket.io-client](https://github.com/socketio/socket.io-client-java) is used for making Socket.IO calls
- [android-beacon-library](https://altbeacon.github.io/android-beacon-library/) is used for discovering AltBeacons

You can include them in your build.gradle file.

## Lambda support

```java
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
```
The Flare library makes extensive use of [lambdas](http://www.oracle.com/webfolder/technetwork/tutorials/obe/java/Lambda-QuickStart/index.html), a feature of Java 8. This [tutorial](http://viralpatel.net/blogs/lambda-expressions-java-tutorial/) explains how lambdas work.

Unfortunately, Android does not yet (as of version 5.1) support Java 8 for application development. However, you can use the [gradle-retrolambda](https://github.com/evant/gradle-retrolambda) plugin to add support for the lambda syntax. You can add these entries to your build.gradle file. 

Make sure that you have installed both the [Java 7 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html) (for Android development) and the [Java 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) (for lambda support). 

# Library documentation 

The Flare library contains several classes:

FlareManager is a high level class with methods for calling both the Flare REST API (for describing objects in the environment) and the Flare Socket.IO API (for realtime communication between objects). See the [Flare API](api.html#java) page for complete documentation.

The Environment, Zone, Thing and Device classes all inherit common variables and methods from the abstract Flare superclass. These objects can be initialized with JSON objects returned by the FlareManager. 

BeaconManager is a class allows an Android device to calculate its position in the environment based on the distance to three or more beacons. 

# Using FlareManager

You can use the FlareManager to calling both the Flare REST API (for describing objects in the environment) and the Flare Socket.IO API (for realtime communication between objects). 

## Init
```java
var flareManager = new FlareManager(host, port);
```
Provide the host and port when initializing the FlareManager.

## Set delegate
```java
flareManager.setActivity(this);
flareManager.setDelegate(this);
```
Set the activity, which is used by Volley to create a request queue.

Set the delegate (which should implement the FlareManager.Delegate interface) to receive SocketIO message callbacks.
	
## Connect
```java
flareManager.connect();
```
Connect to the server before calling the SocketIO interface:
	
## REST interface
```java
flareManager.listEnvironments((jsonArray) -> {
	for (JSONObject json : jsonArray) {
		Environment environment = new Environment(json);
	}
}

flareManager.getDevice(deviceId, environment.getId(), (json) -> {
	Device device = new Device(json);
});
```
To call a REST method:

These methods use lambdas for asynchronous requests. When the response has been received, the lambda is called with a JSONObject or an array of JSONObjects. 
					
## Socket.IO interface
```java
flareManager.getData(environment);
```
To call a SocketIO method.

```java
public void didReceiveData(Flare flare, JSONObject data) {
	Log.d(TAG, flare.getName() + " data: " + data.toString());
}
```
Implement a delegate method to receive SocketIO message callbacks.

For more examples, see the [Socket.IO tutorial](socketio-tutorial.html?java).

# Using FlareBeaconManager

You can use the BeaconManager to determine the location of the user's device based on the distances from the beacons in the environment.

```java
FlareBeaconManager.setDeviceTypeAndConsumer("MOBILE", this);
FlareBeaconManager.setCallback((PointF position) -> {
	runOnUiThread(() -> {
		Log.d(TAG, "Position: " + position);
		flareManager.setPosition(this.device, position);
	});
});
FlareBeaconManager.bind(this);
FlareBeaconManager.setEnvironment(environment);
FlareBeaconManager.restartRangingBeacons();
```
FlareBeaconManager is a singleton class, so all the methods for interacting with it are static methods called on the class itself rather than an instance that you create. 

When setting the callback, you can supply a lambda that will be called every time the device location has been calculated, which will happen up to once per second. 

For a complete example, see the [Beacons](beacons.html?java) tutorial.
