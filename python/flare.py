#!/usr/bin/env python

import requests
from socketIO_client import SocketIO, LoggingNamespace

host = 'localhost'
port = 1234
server = 'http://' + host + ':' + str(port)
socket = SocketIO(host, port, LoggingNamespace)

def get(uri, params={}):
    return requests.get(server + uri, params=params).json()

def post(uri, json): 
    return requests.post(server + uri, json=json).json()

def put(uri, json): 
    return requests.put(server + uri, json=json).json()

def delete(uri): 
    return requests.delete(server + uri).json()


# Environments

def getEnvironments(location={}):
    'Gets a list of environments. Pass a dictionary with latitude and longitude values to filter by location.'
    return get('/environments', params=location)

def newEnvironment(json):
    return post('/environments', json=json)

def getEnvironment(environment_id):
    return get('/environments/' + environment_id)

def updateEnvironment(environment_id, json):
    return put('/environments/' + environment_id, json=json)

def deleteEnvironment(environment_id):
    return delete('/environments/' + environment_id)

# Zones

def getZones(environment_id, position={}):
    'Gets a list of zones. Pass a dictionary with x and y values to filter by position.'
    return get('/environments/' + environment_id + '/zones', params=position)

def newZone(environment_id, json):
    return post('/environments/' + environment_id + '/zones', json=json)

def getZone(environment_id, zone_id):
    return get('/environments/' + environment_id + '/zones/' + zone_id)

def updateZone(environment_id, zone_id, json):
    return put('/environments/' + environment_id + '/zones/' + zone_id, json=json)

def deleteZone(environment_id, zone_id):
    return delete('/environments/' + environment_id + '/zones/' + zone_id)

# Things

def getThings(environment_id, zone_id):
    return get('/environments/' + environment_id + '/zones/' + zone_id + '/things')

def newThing(environment_id, zone_id, json):
    return post('/environments/' + environment_id + '/zones/' + zone_id + '/things', json=json)

def getThing(environment_id, zone_id, thing_id):
    return get('/environments/' + environment_id + '/zones/' + zone_id + '/things/' + thing_id)

def getThingData(environment_id, zone_id, thing_id):
    return get('/environments/' + environment_id + '/zones/' + zone_id + '/things/' + thing_id + '/data')

def getThingDataValue(environment_id, zone_id, thing_id, key):
    return get('/environments/' + environment_id + '/zones/' + zone_id + '/things/' + thing_id + '/data/' + key)

def getThingPosition(environment_id, zone_id, thing_id):
    return get('/environments/' + environment_id + '/zones/' + zone_id + '/things/' + thing_id + '/position')

def updateThing(environment_id, zone_id, thing_id, json):
    return put('/environments/' + environment_id + '/zones/' + zone_id + '/things/' + thing_id, json=json)

def deleteThing(environment_id, zone_id, thing_id):
    return delete('/environments/' + environment_id + '/zones/' + zone_id + '/things/' + thing_id)

# Devices
 
def getDevices(environment_id):
    return get('/environments/' + environment_id + '/devices')

def newDevice(environment_id, json):
    return post('/environments/' + environment_id + '/devices', json=json)

def getDevice(environment_id, device_id):
    return get('/environments/' + environment_id + '/devices/' + device_id)

def getDeviceData(environment_id, device_id):
    return get('/environments/' + environment_id + '/devices/' + device_id + '/data')

def getDeviceDataValue(environment_id, device_id, key):
    return get('/environments/' + environment_id + '/devices/' + device_id + '/data/' + key)

def getDevicePosition(environment_id, device_id):
    return get('/environments/' + environment_id + '/devices/' + device_id + '/position')

def updateDevice(environment_id, device_id, json):
    return put('/environments/' + environment_id + '/devices/' + device_id, json=json)

def deleteDevice(environment_id, device_id):
    return delete('/environments/' + environment_id + '/devices/' + device_id)

# SocketIO sent

def subscribe(message, all=False):
    '''Subscribe to be notified for messages about an object.
    if all is true, also subscribes to all child objects 
    (an environment's zones, things and devices; or a zone's things)
    subscribe(\{'thing': 123\})'''
    if all: message['all'] = True
    socket.emit('subscribe', message)

def unsubscribe(message):
    '''Unsubscribe to no longer be notified for messages about an object.
    unsubscribe(\{'thing': 123\})'''
    socket.emit('unsubscribe', message)

def getData(message, key=None):
    '''Get all data for an object, or a one data value if a key is specified.
    implement gotData() to receive the response
    getData(\{'thing': 123\})'''
    if key != None: message['key'] = key
    socket.emit('getData', message)

def setData(message, key, value, sender=None):
    '''Set a data value for an object.'''
    message['key'] = key
    message['value'] = value
    if sender != None: message['sender'] = sender
    socket.emit('setData', message)

def getPosition(message):
    '''Get the postion of an object.
    implement gotPosition() to receive the response
    # getPosition(\{'thing': 123\})'''
    socket.emit('getPosition', message)

def setPosition(message, position, sender=None):
    '''Set the position of an object.
    setPosition(\{'thing': 123\}, \{x:3, y:4}\)'''
    message['position'] = position
    if sender != None: message['sender'] = sender
    socket.emit('setPosition', message)

def performAction(message, action, sender=None):
    '''Perform an action on an object.
    setData(\{'thing': 123\}, 'launch')'''
    message['action'] = action
    if sender != None: message['sender'] = sender
    socket.emit('performAction', message)

# SocketIO received

class FlareDelegate: 
    def gotData(type, id, data, sender): pass
    def gotPosition(type, id, position, sender): pass
    def enter(device_id, zone_id): pass
    def exit(device_id, zone_id): pass
    def near(device_id, thing_id): pass
    def far(device_id, thing_id): pass
    def handleAction(type, id, action, sender): pass

delegate = None

def getType(message):
    if 'environment' in message: return 'environment'
    elif 'zone' in message: return 'zone'
    elif 'thing' in message: return 'thing'
    elif 'device' in message: return 'device'

def getId(message):
    if 'environment' in message: return message['environment']
    elif 'zone' in message: return message['zone']
    elif 'thing' in message: return message['thing']
    elif 'device' in message: return message['device']

def gotData(*args):
    message = args[0]
    type = getType(message)
    id = getId(message)
    sender = message.get('sender', None)
    if delegate is not None: delegate.gotData(type, id, message['data'], sender)

def gotPosition(*args):
    message = args[0]
    type = getType(message)
    id = getId(message)
    sender = message.get('sender', None)
    if delegate is not None: delegate.gotPosition(type, id, message['position'], sender)
 
def enter(*args):
    message = args[0]
    if delegate is not None: delegate.enter(message['device'], message['zone'])

def exit(*args):
    message = args[0]
    if delegate is not None: delegate.exit(message['device'], message['zone'])

def near(*args):
    message = args[0]
    if delegate is not None: delegate.near(message['device'], message['thing'])

def far(*args):
    message = args[0]
    if delegate is not None: delegate.far(message['device'], message['thing'])

def handleAction(*args):
    message = args[0]
    type = getType(message)
    id = getId(message)
    sender = message.get('sender', None)
    if delegate is not None: delegate.handleAction(type, id, message['action'], sender)
   
socket.on('data', gotData)
socket.on('position', gotPosition)
socket.on('enter', enter)
socket.on('exit', exit)
socket.on('near', near)
socket.on('far', far)
socket.on('handleAction', handleAction)

def wait(): 
    socket.wait()
