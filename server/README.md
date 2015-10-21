# Flare Server

If you have a UNIX-based operating system such as OS X or Linux, you can run the Flare server on your own computer. You can also run the Flare server directly on a server running OS X or Linux. 

If you have a Windows computer, want to run Flare on a server, or just don't want to configure your own runtime environment, you can run Flare in a virtual machine using Vagrant.

## On your computer

### Install Node.js

Install the [Node.js](https://nodejs.org/download) runtime environment. Node.js is a tool that is used to run the Flare server, which is implemented as a JavaScript script. 

### Install MongoDB

Install the [MongoDB](https://www.mongodb.org/downloads) database. MongoDB is used to store data for the Flare server, and must be running in order for the Flare server to connect to it. 

### Change to Flare directory

```all
cd ~/Downloads/flare
```
Open a terminal and change to the Flare API directory, wherever you have downloaded it.

### Install node modules

```all
npm install
```
Then use the node package manager to install the required packages and their dependencies. The npm tool will look in the package.json file to find out which packages need to be installed.

### Start MongoDB

```all
mongod --dbpath data/db
```
In a separate terminal window or tab, launch the MongoDB sever. You can specify the database path using the dbpath flag. The default directory data/db has already been created for you, but you can create a different directory and use that if you want to.

### Initialize the database

```all
node import.js model.json
```
You can initialize the database using the import.js script with some default environments. This may be useful when you are getting started with the Flare API. 

For production use, you'll want to use your own data. See the [environments](environments.html) page for further instructions on setting up your environments. 

### Start the server

```all
node flare.js
```
In a separate terminal window or tab, start the Flare server. This will tell Node.js to run the flare.js script and wait for incoming HTTP requests and Socket.IO messages.

### Start page

You can open the start page by going to [http://localhost:1234/](http://localhost:1234/) in your browser. Substitute the name of your computer to connect from other devices. 

## Virtual machine

### Install Vagrant

Install [Vagrant](https://www.vagrantup.com/downloads.html) to configure the virtualization environment.

### Install VirtualBox

Install the [VirtualBox](https://www.virtualbox.org/wiki/Downloads) vitualization application.

### Change to Flare directory

```all
cd ~/Downloads/flare
```
Open a terminal and change to the Flare API directory.

### Choose a virtual box

```all
vagrant init hashicorp/precise32
```
This command will download a virtual machine image to your computer and set up vagrant to use it.

### Start a virtual machine:

```all
vagrant up
```
This will use the Vagrantfile in the Flare directory to configure and launch the virtual machine, and then install all necessary software.

## Start page

Once vagrant is running, you can open the start page by going to [http://localhost:1234/](http://localhost:1234/) in your browser. Substitute the name of your server to connect from other devices. 