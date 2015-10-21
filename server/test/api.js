// ------------------------------------------------------------ //
// Title: Flare API Mocha Tests
// Author: Simon Dyke
// Date: Oct 2015
// Description: Set of initial mocha tests for the Flare API. It's
//              worth noting that we spin-up a localhost server
//              for these tests, so it is totally standalone and
//              self sufficient.
// TODO: At some point as we develop and enhance the tests we
//              can split some of this out in to seperate files.
// ------------------------------------------------------------ //

// Mocha / Chai assert lib stuff
var expect  = require("chai").expect;
var request = require("request");
var assert = require('chai').assert;
var should = require('chai').should;

//var http = require('http');
var request = require('request');

// Some local config creation for testing if receiving requests for target machine info
var host = process.env.HOSTNAME || 'localhost';
var port = process.env.PORT || '1234';

// Flare API Express server instance
var server = require('../flare');


// ------------------------------------------------------------ //
// Main test entry point
// ------------------------------------------------------------ //

describe("Flare API Test Suite", function() {

    // ------------------------------------------------------------ //
    // Global Mocha hooks
    // ------------------------------------------------------------ //

    // runs before all tests in this block
    before(function() {
        // TODO: tidy-up the server etc before running test suite
    });

    // runs after all tests in this block
    after(function() {
        // TODO: tidy-up the server etc after running test suite
    });

    // runs before each test in this block
    beforeEach(function() {

    });

    // runs after each test in this block
    afterEach(function() {

    });

    // ------------------------------------------------------------ //
    // Environments Tests
    // ------------------------------------------------------------ //
    describe("Environments Test Suite", function() {

        var testEnvironmentId = null;

        var url = "http://" + host + ":" + port + "/environments";

        it('Should create an environment', function (done) {

            var options = {
                uri: url,
                method: 'POST',
                json: {
                    "name": "Test Environment",
                    "description": "Temporary Test Environment",
                    "data": {},
                    "angle": 30,
                    "geofence": {
                        "latitude": 40.751267,
                        "longitude": -73.99229,
                        "radius": 100
                    },
                    "perimeter": {
                        "origin": {
                            "x": 0,
                            "y": 0
                        },
                        "size": {
                            "height": 10,
                            "width": 10
                        }
                    }
                }
            };

            request(options, function (error, res, body) {
                // Should be okay...
                expect(res.statusCode).to.equal(200);
                // error should be null
                expect(error).to.equal(null);

                done();
            });
        });

        it('Should not delete an invalid existing environment', function (done) {
            // Environment id should be null currently at this point in the test suite
            options = {
                uri: url + "/" + testEnvironmentId,
                method: 'DELETE'
            };

            request(options, function (error, res, body) {
                // Should be an error...
                expect(res.statusCode).to.equal(404);
                done();
            });
        });

        it('Should fail to call environment api using PUT', function (done) {
            var options = {
                uri: url,
                method: 'PUT',
                json: null
            };

            request(options, function (error, res, body) {
                // Should be an error...
                expect(res.statusCode).to.equal(404);
                done();
            });
        });

        it('Should fail to create an environment with malformed json body', function (done) {
            var options = {
                uri: url,
                method: 'POST',
                json: { "something" : "random"}
            };

            request(options, function (error, res, body) {
                // Should be an error...
                expect(res.statusCode).to.equal(400);
                done();
            });

        });

        it('Should get a list of environments', function (done) {
            request.get(url, function (err, res, body) {
                // Should be okay...
                expect(res.statusCode).to.equal(200);
                // Body should be an array regardless
                expect(JSON.parse(body)).to.be.a('array');
                // SPD NOTE: Should be able to get an id for an environment which we have just created - save to suite global
                testEnvironmentId = JSON.parse(body)[0]._id;
                done();
            });
        });

        it('Should get info for 1 existing environment', function (done) {
            request.get(url + "/" + testEnvironmentId, function (err, res, body) {
                // Should be okay...
                expect(res.statusCode).to.equal(200);
                done();
            });
        });

        it('Should get environment data for an existing environment', function (done) {
            request.get(url + "/" + testEnvironmentId + "/data", function (err, res, body) {
                // Should be okay...
                expect(res.statusCode).to.equal(200);
                done();
            });
        });


        // ------------------------------------------------------------ //
        // Zones Tests
        // ------------------------------------------------------------ //
        describe("Zones Test Suite", function() {
            var testZoneId = null;

            var url = "http://" + host + ":" + port;

            // example of pending tests below - e.g. tests we need to write at some point
            it('should add tests here');

            // ------------------------------------------------------------ //
            // Things Tests
            // ------------------------------------------------------------ //
            describe("Things Test Suite", function() {
                var url = "http://" + host + ":" + port;

                // example of pending tests below - e.g. tests we need to write at some point
                it('should add tests here');
            });
        });

        // ------------------------------------------------------------ //
        // Devices Tests
        // ------------------------------------------------------------ //
        describe("Devices Test Suite", function() {
            var url = "http://" + host + ":" + port;

            // example of pending tests below - e.g. tests we need to write at some point
            it('should add tests here');
        });

        // ------------------------------------------------------------ //
        // Environment Tests - Tidy Up and Tear-down post-tests
        // ------------------------------------------------------------ //
        it('Should delete an existing environment', function (done) {
            // Environment id should be null currently at this point in the test suite
            options = {
                uri: url + "/" + testEnvironmentId,
                method: 'DELETE'
            };
            request(options, function (error, res, body) {
                // Should be an error...
                expect(res.statusCode).to.equal(200);
                done();
            });
        });


        // example of pending tests below - e.g. tests we need to write at some point
        it('should return -1 when the value is not present');
    });
});

