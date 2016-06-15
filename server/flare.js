// Flare API
// Andrew Zamler-Carhart

// To run the server:
// HOSTNAME=localhost PORT=1234 node flare.js

var flare = this;
var express = require("express"),
	MongoClient = require('mongodb').MongoClient,
	mongoose = require('mongoose'),
	ObjectId = require('mongodb').ObjectID,
	assert = require('assert'),
	flaredb = require("./flaredb"),
	app = express(),
	bodyParser = require('body-parser'),
	errorHandler = require('errorhandler'),
	methodOverride = require('method-override'),
	actions = require('./actions'),
	model = require('./model.json'),
	arp = require('node-arp'),
	hostname = process.env.HOSTNAME || 'localhost',
	port = parseInt(process.env.PORT, 10) || 1234,
	publicDir = process.argv[2] || __dirname + '/public',
	io = require('socket.io').listen(app.listen(port)),
	d = new Date(),
	globalSocket = null,
	defaultMinDistance = 1.5,
	defaultCeiling = 3.0,
	defaultDepth = 0.0, // defaultDepth should be above defaultFloor and below defaultCeiling
	defaultFloor = 0.0;
	
// if false, handleAction messages will only be broadcast if the action is not handled on the server (i.e. implemented in actions.js)
// if true, handleAction messages will be broadcast regardless if they are handled on the server
var broadcastHandledActions = true;

app.use(methodOverride());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
	extended: true
}));
app.use(express.static(publicDir));
app.use(errorHandler({
	dumpExceptions: true,
	showStack: true
}));


// ENVIRONMENTS

app.get('/environments', function (req, res) {
	flaredb.Environment.find(function (err, list) {
	    if (err) return res.send(err);

		var latitude = req.query.latitude;
		var longitude = req.query.longitude;
		if (latitude !== undefined && longitude !== undefined) {
			list = list.filter(function(environment) {
				return insideGeofence(latitude, longitude, environment.geofence);
			});
		}
		
		var key = req.query.key;
		var value = req.query.value;
		if (key !== undefined && value !== undefined) {
			list = list.filter(function(environment) {
				return environment.data != undefined && environment.data[key] == value;
			});
		}
		
	    res.json(list);
	});
});

app.get('/environments/current', function (req, res) {
	flaredb.Environment.find(function (err, list) {
	    if (err) return res.send(err);
	    flaredb.Device.findById(req.query.device, function (err2, device) {
			if (err2) return res.send(err2);
			
			var latitude = device.data.location.latitude;
			var longitude = device.data.location.longitude;
			if (latitude !== undefined && longitude !== undefined) {
				list = list.filter(function(environment) {
					return insideGeofence(latitude, longitude, environment.geofence);
				});
			}
		
		    res.json(list);
		});
	});
});

app.post('/environments', function (req, res) {
	var environment = req.body;
	environment.created = new Date();
	environment.modifed = new Date();
	flaredb.Environment.create(req.body, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});

app.get('/environments/:environment_id', function (req, res) {
	flaredb.Environment.findById(req.params.environment_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});

app.get('/environments/:environment_id/data', function (req, res) {
	flaredb.Environment.findById(req.params.environment_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post.data);
	});
});

app.get('/environments/:environment_id/data/:key', function (req, res) {
	flaredb.Environment.findById(req.params.environment_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post.data[req.params.key]);
	});
});

app.put('/environments/:environment_id', function (req, res) {
	var info = req.body;
	info.modified = new Date();
	flaredb.Environment.findByIdAndUpdate(req.params.environment_id, info, {new: true}, function (err, post) {
		if (err) return res.err(err);
		res.json(post);
	});
});

app.delete('/environments/:environment_id', function (req, res) {
	flaredb.Environment.findByIdAndRemove(req.params.environment_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});


// ZONES

app.get('/environments/:environment_id/zones', function (req, res) {
	flaredb.Zone.find({environment:req.params.environment_id}, function (err, list) {
	    if (err) return res.send(err);

		var x = req.query.x;
		var y = req.query.y;
		var z = req.query.z;
		if (z == undefined) z = defaultDepth;
		
		if (x !== undefined && y !== undefined) {
			var point = {x:x, y:y, z:z};
			list = list.filter(function(zone) {
				return containsPoint(zone.perimeter, point);
			});
		}
		
		var key = req.query.key;
		var value = req.query.value;
		if (key !== undefined && value !== undefined) {
			list = list.filter(function(zone) {
				return zone.data != undefined && zone.data[key] == value;
			});
		}
		
	    res.json(list);
	});
});

app.get('/environments/:environment_id/zones/current', function (req, res) {
	flaredb.Zone.find({environment:req.params.environment_id}, function (err, list) {
		if (err) return res.send(err);
	    flaredb.Device.findById(req.query.device, function (err2, device) {
			if (err2) return res.send(err2);

			list = list.filter(function(zone) {
				return containsPoint(zone.perimeter, device.position);
			});
		
		    res.json(list);
		});
	});
});

app.post('/environments/:environment_id/zones', function (req, res) {
	var zone = req.body;
	zone.environment = req.params.environment_id; // verify that it's a valid object
	zone.created = new Date();
	zone.modifed = new Date();
	flaredb.Zone.create(zone, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});

app.get('/environments/:environment_id/zones/:zone_id', function (req, res) {
	flaredb.Zone.findById(req.params.zone_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});

app.get('/environments/:environment_id/zones/:zone_id/data', function (req, res) {
	flaredb.Zone.findById(req.params.zone_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post.data);
	});
});

app.get('/environments/:environment_id/zones/:zone_id/data/:key', function (req, res) {
	flaredb.Zone.findById(req.params.zone_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post.data[req.params.key]);
	});
});

app.put('/environments/:environment_id/zones/:zone_id', function (req, res) {
	var info = req.body;
	info.modified = new Date();
	flaredb.Zone.findByIdAndUpdate(req.params.zone_id, info, {new: true}, function (err, post) {
		if (err) return res.err(err);
		res.json(post);
	});
});

app.delete('/environments/:environment_id/zones/:zone_id', function (req, res) {
	flaredb.Zone.findByIdAndRemove(req.params.zone_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});


// THINGS

app.get('/environments/:environment_id/zones/:zone_id/things', function (req, res) {
	flaredb.Thing.find({zone:req.params.zone_id}, function (err, list) {
		if (err) return res.send(err);

		var x = req.query.x;
		var y = req.query.y;
		var z = req.query.z;
		if (z == undefined) z = defaultDepth;
    
		var distance = req.query.distance;
		if (x !== undefined && y !== undefined && distance !== undefined) {
			var point = {x:x, y:y, z:z};
			list = list.filter(function(thing) {
				return distanceBetween(thing.position, point) < distance;
			});
		}
		
		var key = req.query.key;
		var value = req.query.value;
		if (key !== undefined && value !== undefined) {
			list = list.filter(function(thing) {
				return thing.data != undefined && thing.data[key] == value;
			});
		}
		
	    res.json(list);
	});
});

app.get('/environments/:environment_id/zones/:zone_id/things/nearby', function (req, res) {
	flaredb.Thing.find({zone:req.params.zone_id}, function (err, list) {
		if (err) return res.send(err);
	    flaredb.Device.findById(req.query.device, function (err2, device) {
			if (err2) return res.send(err2);
			
			list = list.filter(function(thing) {
				return canSeeThing(device, thing);
			});

			res.json(list);
		});
	});
});

app.get('/environments/:environment_id/things', function (req, res) {
	flaredb.Thing.find({environment:req.params.environment_id}, function (err, list) {
	    if (err) return res.send(err);
	    res.json(list);
	});
});

app.post('/environments/:environment_id/zones/:zone_id/things', function (req, res) {
	var thing = req.body;
	thing.environment = req.params.environment_id; // verify that it's a valid object
	thing.zone = req.params.zone_id; // verify that it's a valid object
	thing.created = new Date();
	thing.modifed = new Date();
	flaredb.Thing.create(req.body, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});

app.get('/environments/:environment_id/zones/:zone_id/things/:thing_id', function (req, res) {
	flaredb.Thing.findById(req.params.thing_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});

app.get('/environments/:environment_id/zones/:zone_id/things/:thing_id/data', function (req, res) {
	flaredb.Thing.findById(req.params.thing_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post.data);
	});
});

app.get('/environments/:environment_id/zones/:zone_id/things/:thing_id/data/:key', function (req, res) {
	flaredb.Thing.findById(req.params.thing_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post.data[req.params.key]);
	});
});

app.get('/environments/:environment_id/zones/:zone_id/things/:thing_id/position', function (req, res) {
	flaredb.Thing.findById(req.params.thing_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post.position);
	});
});

app.put('/environments/:environment_id/zones/:zone_id/things/:thing_id', function (req, res) {
	var info = req.body;
	info.modified = new Date();
	flaredb.Thing.findByIdAndUpdate(req.params.thing_id, info, {new: true}, function (err, post) {
		if (err) return res.err(err);
		res.json(post);
	});
});

app.delete('/environments/:environment_id/zones/:zone_id/things/:thing_id', function (req, res) {
	flaredb.Thing.findByIdAndRemove(req.params.thing_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});


// DEVICES

app.get('/environments/:environment_id/devices', function (req, res) {
	flaredb.Device.find({environment:req.params.environment_id}, function (err, list) {
	    if (err) return res.send(err);
		
		var x = req.query.x;
		var y = req.query.y;
		var z = req.query.z;
		if (z == undefined) z = defaultDepth;
		var distance = req.query.distance;
		if (x !== undefined && y !== undefined && distance !== undefined) {
			var point = {x:x, y:y, z:z};
			list = list.filter(function(device) {
				return distanceBetween(device.position, point) < distance;
			});
		}
		
		var key = req.query.key;
		var value = req.query.value;
		if (key !== undefined && value !== undefined) {
			list = list.filter(function(thing) {
				return thing.data != undefined && thing.data[key] == value;
			});
		}

	    res.json(list);
	});
});

app.post('/environments/:environment_id/devices', function (req, res) {
	var device = req.body;
	device.environment = req.params.environment_id; // verify that it's a valid object
	device.created = new Date();
	device.modifed = new Date();
	
	flaredb.Device.create(req.body, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
	
	/* 
	// get the MAC address of the device and add it to the object before saving
	var ipAddress = req.connection.remoteAddress;
	if (ipAddress != null && ipAddress.indexOf('::ffff:') === 0) ipAddress = ipAddress.substring(7);
	arp.getMAC(ipAddress, function(err, mac) {
	    if (!err) {
	        console.log("mac: " + mac);
			device.data.mac = mac;
			flaredb.Device.create(req.body, function (err, post) {
				if (err) return res.send(err);
				res.json(post);
			});
	    } else {
	    	console.log("mac error: " + err);
			flaredb.Device.create(req.body, function (err, post) {
				if (err) return res.send(err);
				res.json(post);
			});
	    }
	});
	*/
});

// CMX notification
app.post('/environments/:environment_id/devices/position', function (req, res) {
	var ignoreNull = true;
	var error = null;
	var notifications = req.body.notifications;
	if (notifications != undefined) {
		for (var i = 0; i < notifications.length; i++) {
			var notification = notifications[i];
			var ssid = notification.ssid;
			if (ssid == null) continue;
			var mac = notification.deviceId;
			// console.log('CMX notification: ' + JSON.stringify(notification));

			if (mac != undefined) {
				var locationCoordinate = notification.locationCoordinate;
				console.log('Device: ' + mac.toLowerCase() + ", position: " + locationCoordinate.x + "," + locationCoordinate.y);
				var query = {environment: req.params.environment_id, "data.mac": mac.toLowerCase()}
				flaredb.Device.find(query, function (err, results) {
					if (err) return res.send(err);
					if (results.length) {
						var device = results[0];
						console.log('Setting position for device: ' + device.name);
						if (locationCoordinate != undefined) {
							var position = {x: locationCoordinate.x, y: locationCoordinate.y};
							device.position = position;
							if (globalSocket != null) {
								notifyPosition(globalSocket, {device: device._id}, device);
							}
							res.json(device);
						} else {
							error = 'No locationCoordinate found.'
						}
					} else {
						error = 'No devices found with environment ' + req.params.environment_id + ' and MAC ' + mac;
					}
				});
			} else {
				error = 'No deviceId found.';
			}
		}
	} else {
		error = 'No notifications found.';
	}
	
	if (error != null) {
		console.log(error);
		res.send(error);
	}
});

app.get('/environments/:environment_id/devices/:device_id', function (req, res) {
	flaredb.Device.findById(req.params.device_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});

app.get('/environments/:environment_id/devices/:device_id/data', function (req, res) {
	flaredb.Device.findById(req.params.device_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post.data);
	});
});

app.get('/environments/:environment_id/devices/:device_id/data/:key', function (req, res) {
	flaredb.Device.findById(req.params.device_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post.data[req.params.key]);
	});
});

app.get('/environments/:environment_id/devices/:device_id/position', function (req, res) {
	flaredb.Device.findById(req.params.device_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post.position);
	});
});

app.put('/environments/:environment_id/devices/:device_id', function (req, res) {
	var info = req.body;
	info.modified = new Date();
	flaredb.Device.findByIdAndUpdate(req.params.device_id, info, {new: true}, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});

app.delete('/environments/:environment_id/devices/:device_id', function (req, res) {
	flaredb.Device.findByIdAndRemove(req.params.device_id, function (err, post) {
		if (err) return res.send(err);
		res.json(post);
	});
});



function reloadClient(){  
	 reloadClient = Function("");
	 io.sockets.emit('reload'); //reload page if the server restarts
	 console.log('Asking client to reload...');
}

console.log("Flare server running at " + hostname + ":" + port);

io.sockets.on('initialload', function (socket) {  
	reloadClient();   								
});

io.sockets.on('connection', function (socket) {
  	socket.on('subscribe', function (message) {
		globalSocket = socket; // save socket reference for broadcasting from REST functions
		getObjectForMessage(message, function(object) {
			if (object != null && object != undefined) { 
				socket.join(object._id);
				
				// if all is true, and the object is an environment or a zone
				if (message.all && (message.environment != undefined || message.zone != undefined)) {
					console.log('subscribe ' + object._id + '-all');
					socket.join(object._id + '-all');
				} else {
					console.log('subscribe ' + object._id);
				}
			}
		});
  	});
	
  	socket.on('unsubscribe', function (message) {
		getObjectForMessage(message, function(object) {
			if (object != null && object != undefined) { 
				console.log('unsubscribe ' + object._id);
				socket.leave(object._id);
				socket.leave(object._id + '-all');
			}
		});
  	});
		
  	socket.on('getData', function (message) {
		getData(message, function(result, object) {
			reply(socket, getId(message), 'data', {data: result});
		});
  	});
	
  	socket.on('setData', function (message) {
		console.log("setData " + JSON.stringify(message));
		setData(socket, message, function(result, object) {
			broadcast(socket, getId(message), 'data', {data: result}, parentIds(object, false), message.sender);
		});
  	});
	
  	socket.on('getPosition', function (message) {
		getPosition(message, function(result, object) {
			reply(socket, getId(message), 'position', {position: result});
		});
  	});

  	socket.on('setPosition', function (message) {
		console.log("setPosition " + JSON.stringify(message));
		setPosition(message, function(result, object) {
			broadcast(socket, getId(message), 'position', {position: result}, parentIds(object, false), message.sender);

			if (message.device != undefined) {
				findNearest(socket, message);
			}
		});
  	});

  	socket.on('getActions', function (message) {
		reply(socket, getId(message), 'actions', {actions: getActions(message)});
  	});
	
  	socket.on('performAction', function (message) {
		performAction(socket, message);
  	});
});

// replies to the sender only
function reply(socket, info, name, message) {
	message[info.classname] = info.id;
	console.log("reply " + name + ": " + JSON.stringify(message));
	socket.emit(name, message);
}

// broadcasts to all other subscribers (and cc: other objects)
function broadcast(socket, info, name, message, cc, sender) {
	message[info.classname] = info.id;
	if (sender != undefined) message.sender = sender;
	console.log("broadcast " + name + ": " + JSON.stringify(message));
	socket.broadcast.to(info.id).emit(name, message);
	if (cc != undefined) notifyAll(socket, cc, name, message);
}

// notifies the sender and all subscribers (and cc: other objects)
function notify(socket, info, name, message, cc, sender) {
	message[info.classname] = info.id;
	if (sender != undefined) message.sender = sender;
	console.log("notify " + name + " " + info.id + ": " + JSON.stringify(message));
	socket.emit(name, message);
	socket.broadcast.to(info.id).emit(name, message);
	if (cc != undefined) notifyAll(socket, cc, name, message);
}

// notifies other objects
function notifyAll(socket, cc, name, message) {
	for (var i = 0; i < cc.length; i++) {
		var id = cc[i];
		console.log('cc: ' + id);
		socket.broadcast.to(id).emit(name, message);
	}
}

// notifies the sender and all subscribers that the data value with the given key has changed
function notifyData(socket, message, object, key) {
	object.set("modified", new Date());
	object.save(function (err, object) {
		var data = {};
		data[key] = object.data[key];
		notify(socket, getId(message), 'data', {data: data}, parentIds(object, false), message.sender);
	});
}

// notifies the sender and all subscribers that the position has changed
function notifyPosition(socket, message, object) {
	object.set("modified", new Date());
	object.save(function (err, object) {
		notify(socket, getId(message), 'position', {position: object.position}, parentIds(object, false), message.sender);
	
		if (message.device != undefined) {
			findNearest(socket, message);
		}
	});
}

// returns the room IDs for the object's parents
function parentIds(object, inclusive) {
	var ids = [];
	if (object != undefined) {
		if (inclusive) ids.push(object._id);
		if (object.environment != undefined) ids.push(object.environment + '-all');
		if (object.zone != undefined) ids.push(object.zone + '-all');
	}
	return ids;
}

actions.notifications = {reply:reply, broadcast:broadcast, notify:notify, notifyData:notifyData, notifyPosition:notifyPosition};

// parses the object ID and classname from the message
// given a message like {thing: 123, foo: 'bar'},
// returns {classname: 'thing', id: 123}
function getId(message) {
	for (var i = 0; i < classnames.length; i++) {
		var classname = classnames[i];
		var id = message[classname];
		if (id != undefined) return {classname: classname, id: id};
	}
	console.log('ERROR no object ID found in message: ' + JSON.stringify(message));
	return null;
}

var classnames = ['environment', 'zone', 'thing', 'device'];

function getObjectWithId(id, classname, callback) {
	if (typeof(callback) !== "function") {
		console.log("getObjectWithId with no callback");
	} else if (id == undefined) {
		callback(null);
	} else if (classname == 'environment') {
		flaredb.Environment.findById(id, function (err, post) {
			if (err) callback(err);
			callback(post);
		});
	} else if (classname == 'zone') {
		flaredb.Zone.findById(id, function (err, post) {
			if (err) callback(err);
			callback(post);
		});
	} else if (classname == 'thing') {
		flaredb.Thing.findById(id, function (err, post) {
			if (err) callback(err);
			callback(post);
		});
	} else if (classname == 'device') {
		flaredb.Device.findById(id, function (err, post) {
			if (err) callback(err);
			callback(post);
		});
	} else {
		callback(null); // ERROR invalid classname
	}
}

function getObjectForMessage(message, callback) {
	var info = getId(message);
	if (info == null) return null;
	var classname = info.classname;
	var id = info.id;
	getObjectWithId(id, classname, callback); 
}

// getData {zone: 456} -> {"product": "kallax", "color": "red"}
// gets all data values for zone 456
// getData {zone: 456, key: "product"} -> {"product": "kallax"}
// gets the product data value for zone 456
function getData(message, callback) {
	getObjectForMessage(message, function(object) {
		if (object == undefined) {
			callback(null); // ERROR object not found
			return;
		}
	
		var data = object.data;
		if (data == undefined) {
			data = {};
			object.data = data;
		}
	
		var key = message.key;
		if (key == undefined) {
			callback(data);
		} else {
			var response = {};
			var value = data[key];
			response[key] = value; // what will this do if value is undefined?
			callback(response, object);
		}
	});
} 

// setData {zone: 456, key: "product", value: "kallax"}
// sets the product data value for zone 456 to "kallax"
function setData(socket, message, callback) {
	getObjectForMessage(message, function(object) {
		if (object == undefined) {
			callback(null); // ERROR object not found
			return;
		}
		
		getObjectWithId(message.sender, 'device', function(sender) {
			var data = object.data;
			if (data == undefined) {
				data = {};
				object.data = data;
			}
	
			var key = message.key;
			if (key == undefined) return; // ERROR no key
	
			var value = message.value;
			if (value == undefined) return; // ERROR no value
	
			if (sender == null || canSetData(sender, object, key)) {
				object.set("data." + key, value); // must use set() because data is a mixed type
				object.set("modified", new Date());
				object.save(function (err, object) {
					if (err) console.error("Error saving data.");
					// else console.log("Saved: " + JSON.stringify(object));

					var result = {};
					result[key] = value;
					callback(result, object);
				});
				
				evaluateDataTriggers(socket, object);
			} else {
				reply(socket, getId(message), 'permissionDenied', {error: "Set data not allowed."});
				console.log("Permission denied.");
			}
		});
	});
}

function getPosition(message, callback) {
	getObjectForMessage(message, function(object) {
		if (object == undefined) {
			callback(null); // ERROR object not found
			return;
		}
	
		var position = object.position;
		if (position == undefined) { // is it reasonable to return 0,0 if the position is undefined?
			position = {x: 0.0, y: 0.0};
			object.position = position;
		}
	
		callback(position, object);
	});
} 

function setPosition(message, callback) {
	getObjectForMessage(message, function(object) {
		if (object == undefined) {
			callback(null); // ERROR object not found
			return;
		}
	
		var position = message.position;
		if (position == undefined) {
			callback(null); 
			return; // ERROR no position in message 
		}
		
		var x = position.x;
		var y = position.y;
		var z = position.z;
		if (x == undefined) return; // ERROR no position.x in message
		if (y == undefined) return; // ERROR no position.y in message
		if (z == undefined) z = defaultDepth;
		position = {x:x, y:y, z:z}; // recreate the position object to make sure no other data is added
		
		var oldPosition = object.position;
		// don't do anything if the old value and the new value are the same
		object.position = position;
		object.set("modified", new Date());
		object.save(function (err, object) {
			if (err) console.error("Error saving position.");
			// else console.log("Saved.");

			callback(position, object);
		});
	});
} 

function findNearest(socket, message) {
	getObjectForMessage(message, function(device) {
		if (device == undefined) {
			return;
		}

		flaredb.Environment.findById(device.environment, function (err, environment) {
			if (environment.data === undefined) environment.data = {};
			var minDistance = environment.data.distance;
			if (minDistance == undefined || minDistance == 0) minDistance = defaultMinDistance;
			// console.log("Min distance: " + minDistance)
	
			var oldNearestId = device.nearest;
			getNearestThing(device, minDistance, function (newNearest) {
				var newNearestId = newNearest ? newNearest._id : undefined;

				if ("" + oldNearestId != "" + newNearestId) {
					if (oldNearestId != undefined) {
						console.log('device ' + device._id + ' far from thing ' + oldNearestId);
						flaredb.Thing.findById(oldNearestId, function (err, oldNearest) {
							notify(socket, getId(message), 'far', {thing: oldNearestId}, parentIds(oldNearest, true));
						});
					}
		
					if (newNearestId != undefined) {
						var distance = distanceBetween(device.position, newNearest.position);
						console.log('device ' + device._id + ' near to thing ' + newNearestId + ' (' + distance + ')');
						notify(socket, getId(message), 'near', {thing: newNearestId, distance: distance}, parentIds(newNearest, true));
					}
		
					device.nearest = newNearestId;
					device.save();
				}
			});

			var oldZoneId = device.zone;
			getZoneForDevice(device, function (newZone) {
				var newZoneId = newZone ? newZone._id : undefined;

				if ("" + oldZoneId != "" + newZoneId) {
					if (oldZoneId != undefined) {
						console.log('device ' + device._id + ' exited zone ' + oldZoneId);
						flaredb.Zone.findById(oldZoneId, function (err, oldZone) {
							notify(socket, getId(message), 'exit', {zone: oldZoneId}, parentIds(oldZone, true));
						});
					}
		
					if (newZoneId != undefined) {
						console.log('device ' + device._id + ' entered zone ' + newZoneId);
						notify(socket, getId(message), 'enter', {zone: newZoneId}, parentIds(newZone, true));
					}
		
					device.zone = newZoneId;
					device.save();
				}
			});
		});
	});
}

// returns the nearest thing to the device that is less than minDistance away
function getNearestThing(device, minDistance, callback) {
	var nearest = null;
	var nearestDistance = -1;

	flaredb.Thing.find({environment:device.environment}, function (err, things) {

		for (var i = 0; i < things.length; i++) {
			var thing = things[i];
			var distance = distanceBetween(device.position, thing.position);
		
			if (distance != -1 && distance < minDistance && 
				(nearestDistance == -1 || distance < nearestDistance)) 
			{
				nearestDistance = distance;
				nearest = thing;
			}
		}
		
		callback(nearest);
	});
}

// returns the zone that contains the point
function getZoneForDevice(device, callback) {
	flaredb.Zone.find({environment:device.environment}, function (err, zones) {

		for (var i = 0; i < zones.length; i++) {
			var zone = zones[i];
			if (containsPoint(zone.perimeter, device.position)) {
				callback(zone);
				return;
			}
		}
		
		callback(null);
	});
}

function distanceBetween(p1, p2) {
	// if p1 and p2 aren't valid points, return -1
	if (p1 == undefined || p1.x == undefined || p1.y == undefined || 
		p2 == undefined || p2.x == undefined || p2.y == undefined) return -1;
	if (p1.z == undefined) p1.z = defaultDepth;
	if (p2.z == undefined) p2.z = defaultDepth;
	
	var dx = p1.x - p2.x;
	var dy = p1.y - p2.y;
	var dz = p1.z - p2.z;
	
	return Math.sqrt(dx * dx + dy * dy + dz * dz);
}

function containsPoint(perimeter, point) {
	if (perimeter.origin.z == undefined) perimeter.origin.z = defaultFloor;
	if (perimeter.size.depth == undefined) perimeter.size.depth = defaultCeiling;
	if (point.z == undefined) point.z = defaultDepth;
	
	return perimeter.origin.x <= point.x && point.x <= perimeter.origin.x + perimeter.size.width &&
		   perimeter.origin.y <= point.y && point.y <= perimeter.origin.y + perimeter.size.height && 
		   perimeter.origin.z <= point.z && point.z <= perimeter.origin.z + perimeter.size.depth;
}

function getActions(message) {
	getObjectForMessage(message, function(object) {
		if (object == undefined) {
			return;
		}
	
		var actions = object.actions;
		if (actions == undefined) {
			actions = [];
			object.actions = actions;
		}
	
		callback(actions);
	});
} 

function performAction(socket, message) {
	getObjectForMessage(message, function(object) {
		if (object == undefined) {
			return;
		}
		
		getObjectWithId(message.sender, 'device', function(sender) {
			var data = object.data;
			if (data == undefined) {
				data = {};
				object.data = data;
			}
		
			var action = message.action;
			if (action == undefined) return false; // ERROR no action in message
	
			// types are an issue here. we are making assumptions that object is a thing and sender is a device!!
			if (sender == null || canPerformAction(sender, object, action)) {
				// look for action handlers
				var handler = actions.handlers[action];
				var handled = false;
		
				if (handler != undefined) {
					console.log('Performing action: ' + action);
					handler(socket, message, object);
					handled = true;
				} 
		
				evaluateActionTriggers(socket, object, action);

				// if not handled by the server, or broadcastHandledActions is true, broadcast message to all observers of the object
				if (!handled || broadcastHandledActions) {
					console.log('Broadcasting action: ' + action);
					broadcast(socket, getId(message), 'handleAction', {action: action}, parentIds(object, false), message.sender);
				}
			} else {
				reply(socket, getId(message), 'permissionDenied', {error: "Perform action not allowed."});
				console.log("Permission denied.");
			}
		});
	});
}

// SECURITY

function canSeeThing(device, thing) {
	// return distanceBetween(device.position, thing.position) < 6;
	return inSameZone(device, thing);

	var allowed = false;
	console.log('Can see thing: ' + allowed);
	return allowed;
}

function canGetData(device, thing, key) {
	// return distanceBetween(device.position, thing.position) < 4;

	var allowed = false;
	console.log('Can get data: ' + allowed);
	return allowed;
}

function canSetData(device, thing, key) {
	return true;
	// return distanceBetween(device.position, thing.position) < 2;

	var allowed = inSameZone(device, thing);
	console.log('Can set data: ' + allowed);
	return allowed;
}

function canPerformAction(device, thing, action) {
	return true;
	
	var allowed = inSameZone(device, thing);
	console.log('Can perform action: ' + allowed);
	return allowed;
}

function inSameZone(device, thing) {
	return "" + device.zone == "" + thing.zone;
}

function deviceInZone(device, zone) {
	return "" + device.zone == "" + zone._id;
}

function deviceInEnvironment(device, environment) {
	var latitude = device.data.location.latitude;
	var longitude = device.data.location.longitude;
	if (latitude === undefined || longitude === undefined) return false;
	return insideGeofence(latitude, longitude, environment);
}

function insideGeofence(latitude, longitude, geofence) {
	var radius = geofence.radius;
	if (radius == undefined || radius < 100) radius = 100;
	var radiusDegrees = radius / 111120.0;
	return closeTo(latitude, geofence.latitude, radiusDegrees) && 
		closeTo(longitude, geofence.longitude, radiusDegrees);
}

function closeTo(value, target, tolerance) {
	return target - tolerance <= value && value <= target + tolerance;
}

function evaluateDataTriggers(socket, object) {
	var triggers = object.data.triggers;
	if (triggers === undefined) return;
	for (var i = 0; i < triggers.length; i++) {
		var trigger = triggers[i];
		if (evaluatePredicate(object, trigger.if)) {
			pullTrigger(socket, object, trigger);
		} 
	}
}

function evaluateActionTriggers(socket, object, action) {
	var triggers = object.data.triggers;
	if (triggers === undefined) return;
	console.log("evaluating triggers: " + JSON.stringify(triggers));
	for (var i = 0; i < triggers.length; i++) {
		var trigger = triggers[i];
		if (trigger.on == action) {
			pullTrigger(socket, object, trigger);
		} 
	}
}

function pullTrigger(socket, object, trigger) {
	if (trigger.performAction !== undefined) {
		var message = trigger.performAction;
		console.log("Trigger performAction: " + JSON.stringify(message));
		performAction(socket, message);
	} else if (trigger.setData !== undefined) {
		var message = trigger.setData;
		console.log("Trigger setData: " + JSON.stringify(message));
		setData(socket, message, function(result, object) {
			notify(socket, getId(message), 'data', {data: result}, parentIds(object, false), message.sender);
		});
	} 
}

// if should be an object like {"key": "temperature", "operator": ">=", "value": 80}
function evaluatePredicate(object, predicate) {
	if (predicate === undefined) return false;
	var lhs = object.data[predicate.key];
	var operator = predicate.operator;
	var rhs = predicate.value;
	
	if (lhs === undefined || rhs === undefined) {
		return false;
	} else if (operator == "==") {
		return lhs == rhs;
	} else if (operator == "!=") {
		return lhs != rhs;
	} else if (operator == "<") {
		return lhs < rhs;
	} else if (operator == ">") {
		return lhs > rhs;
	} else if (operator == "<=") {
		return lhs <= rhs;
	} else if (operator == ">=") {
		return lhs >= rhs;
	} else {
		return false;
	}
}
