// Flare Importer
// Andrew Zamler-Carhart

var flare = this;
var flaredb = require("./flaredb");
var modelPath = process.argv[2] || 'public/home/Home.json';
var model = require('./' + modelPath);
var tasks = 0;

/*
flaredb.Environment.remove({});
flaredb.Zone.remove({});
flaredb.Thing.remove({});
flaredb.Device.remove({});
*/

console.log('Importing from ' + modelPath);

for (var i = 0; i < model.environments.length; i++) {
	createEnvironment(model.environments[i]);
};

function createEnvironment(environment) {
	environment.created = new Date();
	environment.modifed = new Date();

	start();
	flaredb.Environment.create(environment, function (err, newEnvironment) {
		if (err) console.log(err);
		console.log('Environment ' + newEnvironment._id + ': ' + newEnvironment.name);

		for (var j = 0; j < environment.zones.length; j++) {
			createZone(newEnvironment._id, environment.zones[j]);
		}

		if (environment.devices != undefined) {
			for (var k = 0; k < environment.devices.length; k++) {
				createDevice(newEnvironment._id, environment.devices[k]);
			}
		}

		finish();
	});
}

function createZone(environment_id, zone) {
	zone.environment = environment_id;
	zone.created = new Date();
	zone.modifed = new Date();

	start();
	flaredb.Zone.create(zone, function (err, newZone) {
		if (err) console.log(err);
		console.log('Zone ' + newZone._id + ': ' + newZone.name);

		for (var l = 0; l < zone.things.length; l++) {
			createThing(environment_id, newZone._id, zone.things[l]);
		}
		
		finish();
	});
}

function createThing(environment_id, zone_id, thing) {
	thing.environment = environment_id;
	thing.zone = zone_id;
	thing.created = new Date();
	thing.modifed = new Date();
	
	start();
	flaredb.Thing.create(thing, function (err, newThing) {
		if (err) console.log(err);
		console.log('Thing ' + newThing._id + ': ' + newThing.name);

		finish();
	});
}

function createDevice(environment_id, device) {
	device.environment = environment_id;
	device.created = new Date();
	device.modifed = new Date();
	
	start();
	flaredb.Device.create(device, function (err, newDevice) {
		if (err) console.log(err);
		console.log('Device ' + newDevice._id + ': ' + newDevice.name);
		
		finish();
	});
}

function start() {
	tasks++;
}

function finish() {
	tasks--;
	if (tasks == 0) {
		console.log('Done.');
		process.exit(0);
	}
}
