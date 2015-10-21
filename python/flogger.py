# Logs Flare notifications for all objects in the database to the console.
# You append the output to a log file by running the script like this:
#   python3 flogger.py >> flare.log

import flare
import datetime

# position messages can be frequent, so these can be turned off by setting this to False
logPosition = True

# for every environment in the database
for environment in flare.getEnvironments():
    # subscribe to all zones, things and devices in the environment
    flare.subscribe({'environment':environment['_id']}, all=True) 

def flog(*vars):
    'Called with an array of strings whenever an event occurs.'
    'The default implementation prints the values separated by a | '
    'You can modify the function to do anything else with the data.'
    print('|'.join(vars))

class Flogger(flare.FlareDelegate):
    'A class that inherits from the FlareDelegate class to receive Socket.IO messages.'
    'The default implementation of each method does nothing, so all methods are optional.'

    def gotData(self, type, id, data, sender):
        target = getTarget(type, id)
        key = list(data.keys())[0]
        value = str(data[key])
        senderName = getSenderName(sender)
        if sender is None: sender = '-'
        flog(timestamp(), 'data', type, id, target['name'], 'sender', sender, senderName, key, value)
        
    def gotPosition(self, type, id, position, sender):
        if logPosition:
            target = getTarget(type, id)
            value = str(position['x']) + ',' + str(position['y'])
            senderName = getSenderName(sender)
            if sender is None: sender = '-'
            flog(timestamp(), 'position', type, id, target['name'], 'sender', sender, senderName, value)
        
    def enter(self, device_id, zone_id):
        device = getTarget('device', device_id)
        zone = getTarget('zone', zone_id)
        flog(timestamp(), 'enter', 'device', device_id, device['name'], 'zone', zone_id, zone['name'])
        
    def exit(self, device_id, zone_id):
        device = getTarget('device', device_id)
        zone = getTarget('zone', zone_id)
        flog(timestamp(), 'exit', 'device', device_id, device['name'], 'zone', zone_id, zone['name'])
        
    def near(self, device_id, thing_id):
        device = getTarget('device', device_id)
        thing = getTarget('thing', thing_id)
        flog(timestamp(), 'near', 'device', device_id, device['name'], 'thing', thing_id, thing['name'])
        
    def far(self, device_id, thing_id):
        device = getTarget('device', device_id)
        thing = getTarget('thing', thing_id)
        flog(timestamp(), 'far', 'device', device_id, device['name'], 'thing', thing_id, thing['name'])
        
    def handleAction(self, type, id, action, sender):
        target = getTarget(type, id)
        senderName = getSenderName(sender)
        if sender is None: sender = '-'
        flog(timestamp(), 'action', type, id, target['name'], 'sender', sender, senderName, action)

# index of all flare objects that have been loaded
flareIndex = {}

def getTarget(type, id): 
    'Lazy loads the info for an object with a given type and ID from the REST API.'
    target = None
    if id in flareIndex: target = flareIndex[id]
    if target is None: 
        if type == 'environment': target = flare.getEnvironment(id)
        elif type == 'zone': target = flare.getZone('-', id) # environment_id not strictly necessary
        elif type == 'thing': target = flare.getThing('-', '-', id)
        elif type == 'device': target = flare.getDevice('-', id)
        elif type is None: # if we don't know what kind of object it 
            if target is None: target = flare.getDevice('-', id)
            if target is None: target = flare.getThing('-', '-', id)
            if target is None: target = flare.getZone('-', id)
            if target is None: target = flare.getEnvironment(id)
        if target is not None: flareIndex[id] = target
    if target is None: target = {'name':'unknown'}
    return target

def getSenderName(sender):
    if sender is None: return '-'
    info = getTarget(None, sender)
    if info is None: return '-'
    name = info['name']
    if name is None: return '-'
    return name

def timestamp(): 
    'Returns a UTC ISO datetime stamp.'
    return datetime.datetime.utcnow().isoformat()[:-7] + 'Z'

# create a Flogger object and set it as the flare delegate
flare.delegate = Flogger()

# wait for Socket.IO messages
flare.wait()