# web-app-template
Reusable template web app including [Grunt], [Twitter Bootstrap], [AngularJS], [AngularStrap] (Bootstrap for AngularJS), [CoffeeScript], [LESS] and [GreenSock]

### Installation

This web app template uses Grunt as an automation tool.

Grunt and Grunt plugins are installed and managed via npm, the Node.js package manager. Grunt 0.4.x requires stable Node.js versions >= 0.8.0. Odd version numbers of Node.js are considered unstable development versions.

Before setting up Grunt ensure that your npm is up-to-date by running npm update -g npm (this might require sudo on certain systems).

In order to get started, you'll want to install Grunt's command line interface (CLI) globally. You may need to use sudo (for OSX, *nix, BSD etc) or run your command shell as Administrator (for Windows) to do this.

```sh
$ npm install -g grunt-cli
```

### Install dependencies

Dependencies are listed in package.json. The only thing you need to do is to the run the following command in order to download all the dependencies:

```sh
$ npm install
```

### Run the web app

For development, we're running a simple [express] webserver running on [node.js].

The following command does a build (compiles LESS files into CSS files), launches a webserver (which will open a window in your default browser), and watch modifications on specified files or directories to automatically recompile your LESS files and live reload your page when anything changes.

```sh
$ grunt serve
```

You can also specify the port to run the node app on, and the live reload port with the following command:

```sh
$ grunt serve --port 8009 --lrport 1339
```

   [node.js]: <http://nodejs.org>
   [Twitter Bootstrap]: <http://twitter.github.com/bootstrap/>
   [express]: <http://expressjs.com>
   [AngularJS]: <http://angularjs.org>
   [AngularStrap]: <http://mgcrea.github.io/angular-strap/>
   [Grunt]: <http://gruntjs.com/getting-started>
   [LESS]: <http://lesscss.org/>
   [GreenSock]: <http://greensock.com/>