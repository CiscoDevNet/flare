// Flare Reset
// Andrew Zamler-Carhart

// node reset.js environments
// node reset.js zones
// node reset.js things
// node reset.js devices
// node reset.js all

var flare = this;
var flaredb = require("./flaredb");
var command = process.argv[2];

if (command == 'environments' || command == 'all') {
	console.log('Resetting environments...');
	flaredb.Environment.remove({}, function(err) {
            if (err) {
                console.log(err);
            } else {
                console.log('Done.');
            }
		}
	);
}

if (command == 'zones' || command == 'all') {
	console.log('Resetting zones...');
	flaredb.Zone.remove({}, function(err) {
            if (err) {
                console.log(err);
            } else {
                console.log('Done.');
            }
		}
	);
}

if (command == 'things' || command == 'all') {
	console.log('Resetting things...');
	flaredb.Thing.remove({}, function(err) {
            if (err) {
                console.log(err);
            } else {
                console.log('Done.');
            }
		}
	);
}

if (command == 'devices' || command == 'all') {
	console.log('Resetting devices...');
	flaredb.Device.remove({}, function(err) {
            if (err) {
                console.log(err);
            } else {
                console.log('Done.');
            }
		}
	);
}

process.exit(0);
