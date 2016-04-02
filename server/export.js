// Flare Importer
// Andrew Zamler-Carhart

var flare = this;
var flaredb = require("./flaredb");
var modelPath = process.argv[2] || 'model.json';
var model = require('./' + modelPath);
var tasks = 0;

/*
flaredb.Environment.remove({});
flaredb.Zone.remove({});
flaredb.Thing.remove({});
flaredb.Device.remove({});
*/

console.log('Exporting data');

start();
flaredb.Environment.find(function (err, environments) {
	for (var i = 0; i < environments.length; i++) {
		var environment = environments[i];
		console.log('Environment ' + environment.name + ": " + JSON.stringify(environment.perimeter));
	
		start();
		flaredb.Zone.find({environment:environment._id}, function (err, zones) {
			for (var j = 0; j < zones.length; j++) {
				var zone = zones[j];
				console.log('  Zone ' + zone.name + ": " + JSON.stringify(zone.perimeter));

				start();
				flaredb.Thing.find({zone:zone._id}, function (err, things) {
					for (var k = 0; k < things.length; k++) {
						var thing = things[k];
						console.log('    Thing ' + thing.name + ": " + JSON.stringify(thing.position));
						
					}
					finish();
				});
			}
			finish();
		});

		start();
		flaredb.Device.find({environment:environment._id}, function (err, devices) {
			for (var l = 0; l < devices.length; l++) {
				var device = devices[l];
				console.log('  Device ' + device.name + ": " + JSON.stringify(device.position));
				
			}
			finish();
		});
	}
	finish();
});

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
