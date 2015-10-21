# Python

The [Python sample code](../downloads/python.html) contains the following files:

- flare.py: a module with wrappers for the REST and Socket.IO API interfaces
- test.py: a short script that loads all objects in the database and listens for notifications
- import.ph: a script that loads Flare objects from a JSON file and imports them into the server
- flogger.py: a test script that lists for all notifications and logs them to the console

# Setup

Get started by downloading the latest version of [Python](https://www.python.org/downloads/). The Flare sample code was developed using Python 3.5 and may not function correctly on version 2.X.

When using Python 3.X tools on the command line, you may need to call __python3__ for running scritps and __pip3__ for installing packages, rather than __python__ and __pip__.

```python
python --version 
python3 --version 
```
To test what version of Python you have installed, you can call:

# Dependencies 

The flare.py module depends on two libraries, [requests](http://www.python-requests.org/en/latest/) for accessing the REST interface, and [socketIO_client](https://pypi.python.org/pypi/socketIO-client) for accessing the Socket.IO interface. You can install them like this:

```python
sudo pip3 install requests
sudo pip3 install socketIO-client
```

# Flare module 

The flare.py script has wrappers for both the REST and Socket.IO API interfaces. See the [Flare API](api.html#python) page for complete documentation.

You can configure the host and port of your Flare server at the top of the script. The default setting is to connect to localhost on port 1234. 

# REST 

```python
def getEnvironments(location={})
def getZones(environment_id, position={})
def getThings(environment_id, zone_id)
def getDevices(environment_id)
```
The module has wrappers for every REST interface. See the [Flare API](api.html) documentation for complete details. Note that unlike the functions in the sample code for other languages, these functions run synchronously: they block until the response is available, and then return the response. This makes writing a simple script easy, but you may consider calling the API from a background thread when writing a more complex application. 

```python
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
```
For example, here is a short script that prints the ID and name of all environments, zones, things and devices in the database.

# Socket.IO

```python
def subscribe(message, all=False)
def unsubscribe(message)
def getData(message, key=None)
def setData(message, key, value, sender=None)
def getPosition(message)
def setPosition(message, position, sender=None)
def performAction(message, action, sender=None)
```
Sending a Socket.IO message is as simple as calling one of the wrapper functions. 

```python
class FlareDelegate: 
    def gotData(type, id, data, sender): pass
    def gotPosition(type, id, position, sender): pass
    def enter(device_id, zone_id): pass
    def exit(device_id, zone_id): pass
    def near(device_id, thing_id): pass
    def far(device_id, thing_id): pass
    def handleAction(type, id, action, sender): pass
```
The flare.py script defines a FlareDelegate class that includes callback methods for each type of message that you want to receive. The default implementation of each function is empty, so you can override the functions for the messages that you are interested in handling. 

```python
class Flogger(flare.FlareDelegate):
    def gotData(self, type, id, data, sender):
        print(type, id, data)
    def gotPosition(self, type, id, position, sender):
        print(type, id, position)
		
flare.delegate = Flogger()
flare.wait()
```
In the client script, define a class that inherits from flare.FlareDelegate class, and implements some of the callback methods. (Note that in Python, classes can inherit from multiple superclasses.) Set the flare.delegate to an instance of the class. Then call flare.wait(), which will make the script run in a loop that waits indefinitely for Socket.IO messages to arrive. This should be the last line in your script as it will block further execution.

