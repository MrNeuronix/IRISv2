# IRISv2

A new version of IRIS-X (home automation system) with strong modular system.
This is core part of smart home system with no human interface.
Interface is available from **IRISv2-Web** repository.

[![Build Status](https://travis-ci.org/Neuronix2/IRISv2.png?branch=master)](https://travis-ci.org/Neuronix2/IRISv2)

Please remember that this project is in deep alpha and now is mostly for developers than for users.

## Features

* Strong modular system
* AMQP messaging protocol inside (ActiveMQ)
* JavaScript based dynamic scripts support for better automation
* More other stuff :)

## Protocol support

* ZWave
* Noolite

## Install

**mvn package**

In project root directory you will find assembled **IRISv2-linux-release.zip**. Unpack them.
Create database for IRISv2 (**conf/iris-db.sql**).

## Configuration

* Rename **main.property.example** to **main.property** in **/conf** directory
* Change what you need in **main.property**

## Run

Type in command line: **java -jar iris-core.jar**
