import flare

for environment in flare.getEnvironments():
    environment_id = environment['_id']
    print(environment_id + ' - ' + environment['name'])
    
    for zone in flare.getZones(environment_id):
        zone_id = zone['_id']
        print('  ' + zone_id + ' - ' + zone['name']) 

        for thing in flare.getThings(environment_id, zone_id):
            thing_id = thing['_id']
            print('    ' + thing_id + ' - ' + thing['name']) 

    for device in flare.getDevices(environment_id):
        device_id = device['_id']
        print('  ' + device_id + ' - ' + device['name']) 
