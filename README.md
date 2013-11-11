# IRISv2

A new version of IRIS-X with strong modular system, AMPQ communication protocol and RESTful service.
IRISv2 is home automation system with voice control and REST API.

[![Build Status](https://travis-ci.org/Neuronix2/IRISv2.png?branch=master)](https://travis-ci.org/Neuronix2/IRISv2)

## Features

* Strong modular system
* AMPQ protocol used for communication between modules
* RESTful service for data requests
* More other stuff :)

## Install

*mvn package*

In project root directory you will find assembled *IRISv2-linux-release.zip*. Unpack them.

## Configuration

Rename *iris.h2.db.example* to *iris.h2.db.example* in */conf* directory, look to */conf/main.properties*

## Run

Type in command line: *java -jar iris-core.jar*