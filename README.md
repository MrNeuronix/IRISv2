# IRISv2

A new version of IRIS-X with strong modular system and RESTful service.
IRISv2 is home automation system with voice control and REST API.

[![Build Status](https://travis-ci.org/Neuronix2/IRISv2.png?branch=master)](https://travis-ci.org/Neuronix2/IRISv2)

## Features

* Strong modular system
* Own message protocol based on SQL used for communication between modules
* RESTful service for data requests
* More other stuff :)

## Install

**mvn package**

In project root directory you will find assembled **IRISv2-linux-release.zip**. Unpack them.
Create database for IRISv2 (**conf/iris-db.sql**).
For correct operation of the IRISv2 is needed **rec** from package **sox**

## Configuration

* Rename **main.property.example** to **main.property** in **/conf** directory
* Change what you need in **main.property**

## Run

Type in command line: **java -jar iris-core.jar**
