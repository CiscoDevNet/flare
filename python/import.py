import flare
import json

def importData():
	with open("model.json") as json_file:
		data = json.load(json_file)
		
		for environment in data.get('environments', []):
			importEnvironment(environment)

def importEnvironment(source):
	environment = {} 
	environment['name'] = source.get('name', 'untitled')
	environment['description'] = source.get('description', '')
	environment['geofence'] = source.get('geofence', {})
	environment['perimeter'] = source.get('perimeter', {})
	environment['data'] = source.get('data', {})
	environment['angle'] = source.get('angle', 0)
	
	result = flare.newEnvironment(environment)
	environmentId = result['_id']
	print('Imported environment', result['name'])
	
	for zone in source.get('zones', []):
		importZone(environmentId, zone)

	for device in source.get('devices', []):
		importDevice(environmentId, device)

def importZone(environmentId, source):
	zone = {}
	zone['name'] = source.get('name', 'untitled')
	zone['description'] = source.get('description', '')
	zone['perimeter'] = source.get('perimeter', {})
	zone['data'] = source.get('data', {})
	
	result = flare.newZone(environmentId, zone)
	zoneId = result['_id']
	print('Imported zone', result['name'])

	for thing in source.get('things', []):
		importThing(environmentId, zoneId, thing)

def importThing(environmentId, zoneId, source):
	thing = {}
	thing['name'] = source.get('name', 'untitled')
	thing['description'] = source.get('description', '')
	thing['position'] = source.get('position', {'x': 0, 'y': 0})
	thing['data'] = source.get('data', {})
	
	result = flare.newThing(environmentId, zoneId, thing)
	print('Imported thing', result['name'])

def importDevice(environmentId, source):
	device = {}
	device['name'] = source.get('name', 'untitled')
	device['description'] = source.get('description', '')
	device['position'] = source.get('position', {'x': 0, 'y': 0})
	device['data'] = source.get('data', {})
	
	result = flare.newDevice(environmentId, device)
	print('Imported device', result['name'])

importData()