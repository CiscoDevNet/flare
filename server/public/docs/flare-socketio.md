# Flare Socket.IO API

While the Flare REST API is used to model the hierarchy of environment, zone, thing and device objects, the Flare Socket.IO API is used for realtime communication between the objects. 

In the object-oriented programming paradigm, objects have data and code. Similarly, the Flare Socket.IO API lets an application get or set data values, and perform actions. 

Unline REST APIs that use the HTTP request/response loop, Socket.IO messages are one-way and can be sent from the client to the server, or from the server to the client. 


### Object IDs

The Flare Socket.IO APIs relate to Flare objects, including environments, zones, things or devices. In any case, it is necessary for all messages to specify the ID of the object that it refers to. The object is specified using its type and ID, for example:

	subscribe {environment: 123} 
	subscribe {"zone": "456"}
	subscribe {"thing": "789"}
	subscribe {"device": "234"}


### Subscriptions

When an application launches or a web page loads, it creates a socket and connects to the server. Each client can subscribe to one or more Flare objects to receive relevant messages about them. Clients can send a *subscribe* message to start receiving messages about an object, and later send an *unsubscribe* message to stop receiving messages about the object. The connection is stateful, so the server remembers which clients have subscribed to which objects. This prevents clients from receiving messages about objects that they don't care about. 

Clients apps can have socket connection that subscribes to one or more objects. Typically, a client represents a single Flare object: a native app on a mobile phone can represent a device object, and a web app on a computer attached to a big screen could represent a thing. In each case, the app would normally subscribe to the object it represents when it starts up. It is not always necessary to unsubscribe, as closing a connection will unsubscribe automatically and the server will clean up its internal state. 

Clients may also subscribe to other objects for a short time. For example, the app for a device may subscribe to a thing when the device is physically near to the thing, or when the user wants to interact with it. When the device is no longer near the thing or the user has finished interacting, then the app can unsubscribe from that thing. While devices can be notified when they are near or far from a thing, subscribing and unsubscribing is the responsibility of the app and is not automatic. 

Apps on mobile devices are typically put to sleep when they are in the background. It is not meaningful for an app in the background to subscribe to an object, because it is not actually running and won't receive any messages. Therefore, it is better to disconnect the socket before entering the background (which will unsubscribe automatically), and to reconnect and subscribe again when the app returns to the foreground. 

Clients that subscribe to an object will not necessarily receive all messages about the object:

- When a client gets a data value for an object, a data message will be sent back to that client only. 
- When a client changes a data value of an object, all other subscribers will receive a notification. However, the client that sent the original message will not receive a notification because it would be redundant; presumably it already has the latest information. 
- Events that originate from the server (as the result of an action, or as a side effect of a device's position changing) will be sent to all subsribers. 


# Client to server:

These messages are sent from the client to the server. 


### subscribe

When: Sent by the client when it wants to subscribe to an object.

Schema: 
	
	environment | zone | thing | device: string (required)
	all: boolean (optional)

Example: 
	
	{"zone": "456"} 

Result: The client will receive notifications about the object, for example when a data value or the position is changed, or when an action is performed on the object (an not handled by the server). If all is true, messages will also be broadcast to all of an environment's zones, things and devices; and to all of a zone's things.


### unsubscribe

When: Sent by the client when it wants to unsubscribe from an object. 

Schema: 
	
	environment | zone | thing | device: string (required)

Example: 

	{"zone": "456"} 

Result: The client will no longer receive notifications about the object.

Note: It is not necessary to unsubscribe when the client app wants to receive notifications about an object until the connection is closed.


### getData

When: Sent by the client when it wants to get some or all data values for an object. 

Schema: 
	
	environment | zone | thing | device: string (required)
	key: string (optional)

Example: 
	
	{"zone": "456"} 
	{"thing": "789", "key": "color"} 	

Result: The client will be sent a *data* message containing some or all data values for the object. If a key is included in the message, then only the corresponding key/value pair will be returned. Otherwise, all key/value pairs will be returned. For example:

	{"zone": "456", "data": {"page": 3, "mood": "chill"}} 
	{"thing": "789", "data": {"color": "red"}}

Note: This is equivalent to calling the GET /environments/{environment_id}/zones/{zone_id}/things/{thing_id}/data to get all data about a thing, or GET /environments/{environment_id}/zones/{zone_id}/things/{thing_id}/data/{key} to get a specific field. An app can use the Socket.IO or REST interfaces for getting data, depending upon what is more convenient. 


### setData

When: Sent by the client when it wants to change a data value for an object. 

Schema: 
	
	environment | zone | thing | device: string (required)
	key: string (required)
	value: mixed (required)

Example: 
	
	{"thing": "789", "key": "color", "value": "purple"} 	

Result: Other subscribers to the object will receive a *data* message with the key/value pair that has been changed. 

	{"thing": "789", "data": {"color": "purple"}}

Note: The client that sent the original *setData* message will not receive a notification because it would be redundant; presumably it already has the latest information. 


### getPosition

When: Sent by the client when it wants to get the position of a thing or device.

Schema: 
	
	thing | device: string (required)

Example: 
	
	{"device": "234"} 

Result: The client will be sent a *position* message containing the position of the object. For example:

	{"device": "234", "position": {"x": 2.3, "y": 3.4}}

Note: This is equivalent to calling the GET /environments/{environment_id}/zones/{zone_id}/things/{thing_id}/position to get the position of a thing. An app can use the Socket.IO or REST interfaces for getting data, depending upon what is more convenient. 


### setPosition

When: Sent by the client when it wants to change the position of an object. 

Schema: 
	
	thing | device: string (required)
	position: object (required)
		x: number (required)
		y: number (required)

Example: 
	
	{"device": "234", "position": {"x": 4.5, "y": 5.6}}

Result: Other subscribers to the object will receive a *position* message with the new position. For example:

	{"device": "234", "position": {"x": 4.5, "y": 5.6}}

Note: The client that sent the original *setPosition* message will not receive a notification because it would be redundant; presumably it already has the latest information. 

This may trigger a *near* or *far* message if a device becomes near to a thing, or is no longer near to a thing. See the documentation below for those messages. 


### performAction

When: Sent by the client when it wants to perform an action on an object. Actions are identified by strings, and the list of possible actions for an object is contained in the actions property, accessible using the REST API. 

Schema: 
	
	environment | zone | thing | device: string (required)
	action: string (required)

Example: 
	
	{"zone": "234", "action": "next"}

Result: The action can be handled by the server or the client. For the server to handle the action, a function with the name of the action should be defined in the file actions.js on the server. The function can interact with the specified object or other objects (by manipulating them directly), and may case other messages to be sent. 

If the server does not handle the action, then a *handleAction* message will be sent to all subscribers of the object, including the sender. For example:

	{"zone": "234", "action": "next"}

The client can then interact with the specified object or other objects (by sending more messages to the server). 

Note: Actions do not have input or output values, but they can operate on data values. For example, an action can optionally use one or more data values as input, and can optionally use one or more data values as output. For example, a "next" message could increment the "page" value, which would cause a data message to be sent to subscribers of the object. 

You don't need to use an action to simply change a data value. You can use *setData* to set the value directly, and any subscribers for the object will be notified of the change. For example, if the current page is 3, instead of performing a next action you could call *setData* like this:

	{"zone": "234", "key": "page", "value": 4} 	


# Server to client:

These messages are sent from the server to the client, usually in response to other messages that have been sent to the server. Depending upon the event that triggered the message, the could be sent to the sender of the original message, to all other subscribers of an object, or to all subscribers of an object (including the sender).


### data

When: Sent by the server to a client who has requested the current data by sending a *getData* message, _or_ to all other subscribers when a client has changed a data value by sending a *setData* message. 

Schema: 
	
	environment | zone | thing | device: string
	data: object

Example: 
	
	{"zone": "456", "data": {"page": 3, "mood": "chill"}} 
	{"thing": "789", "data": {"color": "red"}}

Note: If a key was included in the *getData* message, then only the corresponding key/value pair will be returned. Otherwise, all key/value pairs will be returned.


### position

When: Sent by the server to a client who has requested the current position by sending a *getPosition* message, _or_ to all other subscribers when a client has changed the current position by sending a *setPosition* message.

Schema: 
	
	thing | device: string
	position: object
		x: number
		y: number

Example: 
	
	{"device": "234", "position": {"x": 2.3, "y": 3.4}}


### handleAction

When: Sent by the server to all subscribers of an object when a client has sent a *performAction* message, _and_ the server did not handle the message. The client may then take any appropriate action. 

Schema: 
	
	environment | zone | thing | device: string
	action: string

Example: 
	
	{"thing": "789", "action": "buy"} 


### near

When: Sent by the server when a device has become near to a thing, as a result of a *setPosition* call for the device. The message is sent to all subscribers of the device _and_ all subscribers of the thing. A device is considered near to a device when the diagonal distance is less than a certain threshold defined by the server (by default, 1 meter).

Schema:

	device: string
	thing: string
	distance: number

Example:

	{"device": "234", "thing": "789", "distance": 0.91} 

Note: When a device and a thing are nearby, they can interact. Typically, the client representing the device can subscribe to the thing, and the client representing the thing can subscribe to the device. 

A device is only considered to be near to one thing at a time, so if it is within the given threshold of several devices, it is considered to only be near to the closest one. A thing, on the other hand, may have several devices that are near it. 

The device may continue to move around inside a circle around the thing defined by the minimum threshold. A *near* message will only be sent when a device enters the circle, and a *far* message will only be sent when the devices leaves the circle (unless the devices becomes closer to another thing). 

The distance value in the *near* message is the distance between the device and the thing at the time that the server received a *setPosition* message that caused it to become near to the thing. If the client representing the thing subscribes to the device, then it can continue to receive *position* updates for the device as it moves around. It should check the ID of the object in the *position* message to know if it applies to the thing or a nearby device. 


### far

When: Sent by the server when a device is no longer near to a thing, as a result of a *setPosition* call for the device. The message is sent to all subscribers of the device _and_ all subscribers of the thing.

Schema:

	device: string
	thing: string
	distance: number

Example:

	{"device": "234", "thing": "789", "distance": 0.91} 

Note: When a device and a thing are no longer nearby, they may not want to interact anymore. Typically, the client representing the device can unsubscribe from the thing, and the client representing the thing can unsubscribe from the device. 

