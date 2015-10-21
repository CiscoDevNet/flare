var MongoClient = require('mongodb').MongoClient;
var mongoose = require('mongoose');
var ObjectId = require('mongodb').ObjectID;
var assert = require('assert');

var url = 'mongodb://localhost:27017/test';
mongoose.connect(url);

var EnvironmentSchema = new mongoose.Schema({
	name: String,
	description: String,
	data: mongoose.Schema.Types.Mixed,
	created: {type: Date, default: Date.now},
	modified: {type: Date, default: Date.now},
	actions: [String],
	angle: Number,
	geofence: {
		latitude: Number,
		longitude: Number,
		radius: Number
	},
	perimeter: {
		origin: {x: Number, y: Number},
		size: {width: Number, height: Number}
	}
});

var ZoneSchema = new mongoose.Schema({
	environment: {type: mongoose.Schema.ObjectId, ref: 'Environment'},
	name: String,
	description: String,
	data: mongoose.Schema.Types.Mixed,
	created: {type: Date, default: Date.now},
	modified: {type: Date, default: Date.now},
	actions: [String],
	perimeter: {
		origin: {x: Number, y: Number},
		size: {width: Number, height: Number}
	}
});

var ThingSchema = new mongoose.Schema({
	environment: {type: mongoose.Schema.ObjectId, ref: 'Environment'},
	zone: {type: mongoose.Schema.ObjectId, ref: 'Zone'},
	name: String,
	description: String,
	data: mongoose.Schema.Types.Mixed,
	created: {type: Date, default: Date.now},
	modified: {type: Date, default: Date.now},
	actions: [String],
	position: {x: Number, y: Number}
});

var DeviceSchema = new mongoose.Schema({
	environment: {type: mongoose.Schema.ObjectId, ref: 'Environment'},
	name: String,
	description: String,
	data: {},
	created: {type: Date, default: Date.now},
	modified: {type: Date, default: Date.now},
	actions: [String],
	position: {x: Number, y: Number},
	nearest: {type: mongoose.Schema.ObjectId, ref: 'Thing'},
	zone: {type: mongoose.Schema.ObjectId, ref: 'Zone'}
});

var Environment = mongoose.model('Environment', EnvironmentSchema);
var Zone = mongoose.model('Zone', ZoneSchema);
var Thing = mongoose.model('Thing', ThingSchema);
var Device = mongoose.model('Device', DeviceSchema);

flaredb = {Environment:Environment, Zone:Zone, Thing:Thing, Device:Device};
module.exports = flaredb;