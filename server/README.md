# Getting started

You can run the Flare API on your own computer, or in a virtual machine.

#### To run the server on your own computer:

Install Node.js:

	https://nodejs.org/download/

Install MongoDB:

	https://www.mongodb.org/downloads

Install node modules:

	npm install

Start MongoDB:

	mongod --dbpath data/db

Initialize the database:

	node import.js model.json

Start the flare server:

	node flare.js


#### To run the server in a virtual machine:


Install Vagrant:

	https://www.vagrantup.com/downloads.html
	
Install VirtualBox:

	https://www.virtualbox.org/wiki/Downloads
	
Choose a virtual box:	
	
	vagrant init hashicorp/precise32

Start a virtual machine:

	vagrant up
	
	
# Demo

	
Open the start page in a browser:

	http://localhost:1234/

You will see an outline of environments, zones, things and user devices. 

Then click on any thing or device to open it in a new tab. You can use the direction buttons to move a device around. When it is within (1,1) meters of a thing, the device and the thing will be synced up.
