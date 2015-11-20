var sockethost = location.hostname;
var socket = io.connect(sockethost); //set this to the ip address of your node.js server

socket.emit('initialload', '');

$['put'] = function(url, data, callback) {
	return jQuery.ajax({
		url: url,
		type: 'PUT',
		dataType: 'json',
		data: data,
		success: callback
	});
};

$['delete'] = function(url, callback) {
	return jQuery.ajax({
		url: url,
		type: 'DELETE',
		dataType: 'json',
		data: null,
		success: callback
	});
};

// Environments

function getEnvironments(callback) {
	$.getJSON('/environments', callback);
}

function getEnvironmentsWithParams(params, callback) {
	$.getJSON('/environments?' + $.param(params), callback);
}

function newEnvironment(data, callback) {
	$.post('/environments', data, callback, 'json');
}

function getEnvironment(environment_id, callback) {
	$.getJSON('/environments/' + environment_id, callback);
}

function updateEnvironment(environment_id, data, callback) {
	$.put('/environments/' + environment_id, data, callback);
}

function deleteEnvironment(environment_id, callback) {
	$.delete('/environments/' + environment_id, callback);
}

// Zones

function getZones(environment_id, callback) {
	$.getJSON('/environments/' + environment_id + '/zones', callback);
}

function getZonesWithParams(params, environment_id, callback) {
	$.getJSON('/environments/' + environment_id + '/zones?' + $.param(params), callback);
}

function newZone(environment_id, data, callback) {
	$.post('/environments/' + environment_id + '/zones', data, callback, 'json');
}

function getZone(environment_id, zone_id, callback) {
	$.getJSON('/environments/' + environment_id + '/zones/' + zone_id, callback);
}

function updateZone(environment_id, zone_id, data, callback) {
	$.put('/environments/' + environment_id + '/zones/' + zone_id, data, callback);
}

function deleteZone(environment_id, zone_id, callback) {
	$.delete('/environments/' + environment_id + '/zones/' + zone_id, callback);
}

// Things

function getThings(environment_id, zone_id, callback) {
	$.getJSON('/environments/' + environment_id + '/zones/' + zone_id + '/things', callback);
}

function getThingsWithParams(environment_id, zone_id, params, callback) {
	$.getJSON('/environments/' + environment_id + '/zones/' + zone_id + '/things?' + $.param(params), callback);
}

function newThing(environment_id, zone_id, data, callback) {
	$.post('/environments/' + environment_id + '/zones/' + zone_id + '/things', data, callback, 'json');
}

function getThing(environment_id, zone_id, thing_id, callback) {
	$.getJSON('/environments/' + environment_id + '/zones/' + zone_id + '/things/' + thing_id, callback);
}

function getThingData(environment_id, zone_id, thing_id, callback) {
	$.getJSON('/environments/' + environment_id + '/zones/' + zone_id + '/things/' + thing_id + '/data', callback);
}

function getThingPosition(environment_id, zone_id, thing_id, callback) {
	$.getJSON('/environments/' + environment_id + '/zones/' + zone_id + '/things/' + thing_id + '/position', callback);
}

function updateThing(environment_id, zone_id, thing_id, data, callback) {
	$.put('/environments/' + environment_id + '/zones/' + zone_id + '/things/' + thing_id, data, callback);
}

function deleteThing(environment_id, zone_id, thing_id, callback) {
	$.delete('/environments/' + environment_id + '/zones/' + zone_id + '/things/' + thing_id, callback);
}

// Devices

function getDevices(environment_id, callback) {
	$.getJSON('/environments/' + environment_id + '/devices', callback);
}

function getDevicesWithParams(environment_id, params, callback) {
	$.getJSON('/environments/' + environment_id + '/devices?' + $.param(params), callback);
}

function newDevice(environment_id, data, callback) {
	$.post('/environments/' + environment_id + '/devices', data, callback, 'json');
}

function getDevice(environment_id, device_id, callback) {
	$.getJSON('/environments/' + environment_id + '/devices/' + device_id, callback);
}

function getDeviceData(environment_id, device_id, callback) {
	$.getJSON('/environments/' + environment_id + '/devices/' + device_id + '/data', callback);
}

function getDevicePosition(environment_id, device_id, callback) {
	$.getJSON('/environments/' + environment_id + '/devices/' + device_id + '/position', callback);
}

function updateDevice(environment_id, device_id, data, callback) {
	$.put('/environments/' + environment_id + '/devices/' + device_id, data, callback);
}

function deleteDevice(environment_id, device_id, callback) {
	$.delete('/environments/' + environment_id + '/devices/' + device_id, callback);
}

// subscribe to be notified for messages about an object
// if all is true, also subscribes to all child objects 
// (an environment's zones, things and devices; or a zone's things)
// subscribe({thing: 123})
function subscribe(message, all) {
	if (all) message.all = true;
	socket.emit('subscribe', message);
}

// unsubscribe to no longer be notified for messages about an object
// unsubscribe({thing: 123})
function unsubscribe(message) {
	socket.emit('unsubscribe', message);
}

// get all data for an object
// implement gotData() to receive the response
// getData({thing: 123})
function getData(message) {
	socket.emit('getData', message);
}

// get one data value for an object
// implement gotData() to receive the response
// getData({thing: 123})
function getData(message, key) {
	message.key = key;
	socket.emit('getData', message);
}

// set a data value for an object
// setData({thing: 123}, 'volume', 11)
function setData(message, key, value, sender) {
	message.key = key;
	message.value = value;
	if (sender) message.sender = sender;
	console.log('setData: ' + JSON.stringify(message));
	socket.emit('setData', message);
}

// get the postion of an object
// implement gotPosition() to receive the response
// getPosition({thing: 123})
function getPosition(message) {
	socket.emit('getPosition', message);
}

// set the position of an object
// setPosition({thing: 123}, {x:3, y:4})
function setPosition(message, position, sender) {
	message.position = position;
	if (sender) message.sender = sender;
	socket.emit('setPosition', message);
}

// perform an action on an object
// setData({thing: 123}, 'launch')
function performAction(message, action, sender) {
	message.action = action;
	if (sender) message.sender = sender;
	socket.emit('performAction', message);
}

// implement the gotData() function to be notified when an object's data changes
socket.on('data', function (message) {
	if (typeof(gotData) == 'function') {
		gotData(message);
	}
});

// implement the gotPosition() function to be notified when an object's position changes
socket.on('position', function (message) {
	if (typeof(gotPosition) == 'function') {
		gotPosition(message);
	}
});

// implement the handleAction() function to be notified when an action is performed on an object
socket.on('handleAction', function (message) {
	if (typeof(handleAction) == 'function') {
		handleAction(message);
	}
});

// implement the near() function to be notified when a device becomes near to a thing
socket.on('near', function (message) {
	if (typeof(near) == 'function') {
		near(message);
	}
});

// implement the far() function to be notified when a device is no longer near to a thing
socket.on('far', function (message) {
	if (typeof(far) == 'function') {
		far(message);
	}
});

// implement the enter() function to be notified when a device enters a zone
socket.on('enter', function (message) {
	if (typeof(enter) == 'function') {
		enter(message);
	} else {
		console.log("Enter type: " + typeof(enter));
	}
});

// implement the exit() function to be notified when a device exits a zone
socket.on('exit', function (message) {
	if (typeof(exit) == 'function') {
		exit(message);
	} else {
		console.log("Exit type: " + typeof(exit));
	}
});

function distanceBetween(p1, p2) {
	// if p1 and p2 aren't valid points, return -1
	if (p1 == undefined || p1.x == undefined || p1.y == undefined || 
		p2 == undefined || p2.x == undefined || p2.y == undefined) return -1;
		
	return Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
}

function getParameterByName(name) {
    var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
}
