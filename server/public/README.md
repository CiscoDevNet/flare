# JavaScript

By default the sample web apps are served by the Flare server, and connect to it on localhost. You can find them in the public directory.

## Setup

First, make sure you've got the [Flare server](../) running. 

You can run the sample apps on your own computer by connecting to the Flare server in a browser at [http://localhost:1234/](http://localhost:1234/) and then clicking on one of the links, such as:

- [Explorer](explorerweb.html), an app for browsing and editing Flare objects
- [Socket.IO API Console](http://localhost:1234/console.html), a page where you can send and receive Socket.IO messages

You can find the source code for these examples in the public directory inside the Flare folder. 

## Dependencies 

```all
<script src="js/socket.io-1.0.0.js"></script>
<script src="js/jquery-2.1.4.min.js"></script>
<script src="js/jquery.cookie.js"></script>
<script src="js/flare.js"></script>
```
The Flare web apps depend on a few JavaScript libraries:

- socket.io:
- jquery: a general purpose 
- jquery.cookie: 

These are included in the public/js folder, and can be added to web pages as shown.

### Flare script 

```all
<script src="js/flare.js"></script>
```
The flare.js script includes wrapper functions for all Flare REST and Socket.IO API interfaces, and can be added to web pages as shown. See the [Flare API](api.html#javascript) page for complete documentation.

By default, it is assumed that Flare web apps will be served by the Flare server itself, so by default the flare.js script makes connections to the same server that is serving the current page, on the same port.

## REST 

```javascript
function getEnvironments(callback)
function getZones(environment_id, callback)
function getThings(environment_id, zone_id, callback)
function getDevices(environment_id, callback)
```
The script has wrappers for every REST interface. See the [Flare API](api.html) documentation for complete details. All requests run asynchronously, return immediately, and take a callback function as the final argument that will be called when the request has completed. The callback function takes a single argument, the parsed JSON object or array returned by the API call. 

```javascript
getEnvironments(function(environments) {
	for (var i = 0; i < environments.length; i++) {
		var environment = environments[i];
		var environmentId = environment._id;
		var environmentName = environment.name;
		
		getZones(environmentId, function(zones) {
			for (var j = 0; j < zones.length; j++) {
				var zone = zones[j];
				var zoneId = zone._id;
				var zoneName = zone.name;
				
				getThings(environmentId, zoneId, function(things) {
					for (var k = 0; k < things.length; k++) {
						var thing = things[k];
						var thingId = thing._id;
						var thingName = thing.name;
					}
				});
			}
		});
	}
});
```
For example, here is a short script that gets the ID and name of all environments, zones, things in the database. Note that since the nested functions are called asynchronously, the various statements could be called in any order.

## Socket.IO

```javascript
function subscribe(message, all)
function unsubscribe(message)
function getData(message) 
function getData(message, key)
function setData(message, key, value, sender)
function getPosition(message)
function setPosition(message, position, sender)
function performAction(message, action, sender)
```
Sending a Socket.IO message is as simple as calling one of the wrapper functions. The first argument to each function is a JSON object containing the type and ID of the object, such as {environment:'123'}, {thing:'789'}, etc. 

```javascript
function gotData(message)
function gotPosition(message)
function handleAction(message)
function near(message)
function far(message)
function enter(message)
function exit(message)
```
The flare.js script defines some callback functions that will be called when certain messages are received, if you have implemented them in your script. The callback functions take one argument, the raw JSON message from the server. It is the responsiblity of the client to parse the messages, although the native JSON format makes this easy.

```javascript
var environment_id = '123';
var zone_id = '456';
var thing_id = '789';

getThing(environment_id, zone_id, thing_id, function(info) {
	console.log("name: " + thing.name);
	console.log("data: " + thing.description);
});

subscribe({thing:thing_id});
getData({thing:thing_id});
getPosition({thing:thing_id});

function gotData(message) {
	if (message.thing == thing_id) {
		console.log("data: " + JSON.stringify(message.data));
	}
}

function gotPosition(message) {
	if (message.thing == thing_id) {
		console.log("position: " + JSON.stringify(message.position));
	}
}
```
Here is a short script that will get the name and description of a thing from the REST API. When the client calls getData(), the server will send a data message to the client, which will trigger a gotData() callback. The same thing happens with getPosition() and gotPosition(). And because the client has subscribed to the thing, it will also receive data and position messages wheneve these are changed by another client. 
