pressureNET SDK
===============

This SDK supports [pressureNET](https://play.google.com/store/apps/details?id=ca.cumulonimbus.barometernetwork) ([source](https://github.com/CbSoftware/pressureNET)), an Android app that collects atmospheric pressure data from mobile devices that have built-in barometers. Since pressureNET began as a single app, there was no separation of core data-submission functionality and user-level app features. Additionally, network growth was limited by app growth which is not deal. 

This project is an in-progress Android library that you can include in your apps to enable automatic atmospheric data collection. The code is not stable yet but many features are completed. 

The pressureNET SDK is intended for data collection. We have also built a pressureNET API for data dissemenation that is stable and in active use by scientsts and researchers. We provide a [visualzation of the live pressureNET data](http://pressurenet.cumulonimbus.ca/) as well as an explanation and documentation for the [Live API](http://pressurenet.cumulonimbus.ca/livestream/). Of course, that project is [open source](https://github.com/JacobSheehy/pressureNETAnalysis) too.

Installation
------------

To include this SDK in your Android project, in Eclipse open the Project Properties of the target app. In the Java Build Path page, under the Source tab, use the Link Source feature to include the 'src' directory of this library.

Usage
-----

Although the library is not stable yet, we currently envision usage to be very simple. We expect that all interaction will be done through the CbService class, where you can set parameter values and direct the behavior of the library. Here's a simple example that currently works in a test environment (but please note, this is very early-stage software and this is subject to change).

    Intent intent  = new Intent(getApplicationContext(), CbService.class);
    startService(intent);
    
    
About
-----

This library is developed by [Cumulonimbus](http://cumulonimbus.ca) as a part of the pressureNET project. Our primary goals are to improve short-term, local weather forecasting by dramatically increasing the data inputs available to weather models. The pressureNET servers currently receive about 300,000 atmospheric pressure measurements per day, and with the completetion of this SDK we intend to increase this dramatically.
