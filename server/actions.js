exports.handlers = {};
exports.notifications = {};

exports.handlers['north'] = function(socket, message, object) {
	object.position.y++;
	exports.notifications.notifyPosition(socket, message, object);
};

exports.handlers['south'] = function(socket, message, object) {
	object.position.y--;
	exports.notifications.notifyPosition(socket, message, object);
};

exports.handlers['east'] = function(socket, message, object) {
	object.position.x++;
	exports.notifications.notifyPosition(socket, message, object);
};

exports.handlers['west'] = function(socket, message, object) {
	object.position.x--;
	exports.notifications.notifyPosition(socket, message, object);
};

exports.handlers['up'] = function(socket, message, object) {
	object.position.z++;
	exports.notifications.notifyPosition(socket, message, object);
};

exports.handlers['down'] = function(socket, message, object) {
	object.position.z--;
	exports.notifications.notifyPosition(socket, message, object);
};

var defaultColors = ['red','orange','yellow','green','blue','purple'];

exports.handlers['previousColor'] = function(socket, message, object) {
	var colors = object.data.options
	if (colors == undefined) colors = defaultColors
	var index = colors.indexOf(object.data.color);
	if (index != -1) {
		index--;
		if (index < 0) index += colors.length
		index = index % colors.length;
		object.set('data.color', colors[index]); // must use this syntax to trigger change!
		exports.notifications.notifyData(socket, message, object, 'color');
	}
};

exports.handlers['nextColor'] = function(socket, message, object) {
	var colors = object.data.options
	if (colors == undefined) colors = defaultColors
	var index = colors.indexOf(object.data.color);
	if (index != -1) {
		index++;
		index = index % colors.length;
		object.set('data.color', colors[index]); // must use this syntax to trigger change!
		exports.notifications.notifyData(socket, message, object, 'color');
	}
};

exports.handlers['lighter'] = function(socket, message, object) {
	var brightness = (object.data.brightness * 10.0 + 1.0) / 10.0;
	if (brightness > 1.0) brightness = 1.0;
	object.set('data.brightness', brightness); 
	exports.notifications.notifyData(socket, message, object, 'brightness');
};

exports.handlers['darker'] = function(socket, message, object) {
	var brightness = brightness = (object.data.brightness * 10.0 - 1.0) / 10.0;
	if (brightness < 0.0) brightness = 0.0;
	object.set('data.brightness', brightness); 
	exports.notifications.notifyData(socket, message, object, 'brightness');
};

exports.handlers['counterclockwise'] = function(socket, message, object) {
	var angle = object.data.angle - 30;
	if (object.data.angle < 0) angle += 360;
	object.set('data.angle', angle); 
	exports.notifications.notifyData(socket, message, object, 'angle');
};

exports.handlers['clockwise'] = function(socket, message, object) {
	var angle = object.data.angle + 30;
	if (angle >= 360) angle -= 360;
	object.set('data.angle', angle); 
	exports.notifications.notifyData(socket, message, object, 'angle');
};

exports.handlers['on'] = function(socket, message, object) {
	object.set('data.on', true);
	exports.notifications.notifyData(socket, message, object, 'on');
};

exports.handlers['off'] = function(socket, message, object) {
	object.set('data.on', false);
	exports.notifications.notifyData(socket, message, object, 'on');
};

