# IRISv2

A new version of IRIS-X (home automation system) with strong modular system.
This is core part of smart home system with no human interface.
Interface is available from **IRISv2-Web** repository.

[![Build Status](https://travis-ci.org/Neuronix2/IRISv2.png?branch=master)](https://travis-ci.org/Neuronix2/IRISv2)

Please remember that this project is in deep alpha and now is mostly for developers than for users.

## Features

* Extensible plugin system
* AMQP messaging protocol inside (RabbitMQ)
* JavaScript based dynamic scripts support for better automation
* More other stuff :)

## Protocol support

* ZWave
* Noolite

## Requirements

* JDK 8
* RabbitMQ server
* Linux OS (work with Windows not tested)

## Install

**mvn package**

In project root directory you will find assembled **IRISv2-linux-release.zip**. Unpack them.
Database for IRISv2 will be created dynamically at first run.

## Configuration

* Rename **main.property.example** to **main.property** in **/conf** directory
* Rename **log4j2.property.example** to **log4j2.property** in **/conf** directory
* Change what you need in **main.property** and **log4j2.property**

## Run

Type in command line: **java -jar iris-core.jar** or use **conf/irisd** script

## Licence

Apache 2.0
