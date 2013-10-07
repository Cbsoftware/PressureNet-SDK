pressureNET SDK
==============

The pressureNET SDK is an Android library project that enables simple atmosphere sensor data collection and transmission to researchers. This SDK was created by [Cumulonimbus](http://cumulonimbus.ca) for use in [pressureNET](https://play.google.com/store/apps/details?id=ca.cumulonimbus.barometernetwork) and third-party Android apps. The code is open source and released under the MIT license. 

This SDK is intended for data collection. We have also built a pressureNET API for data dissemination that is actively used by scientists and researchers. We provide a [visualzation of the live pressureNET data](http://pressurenet.cumulonimbus.ca/) as well as an explanation and documentation for the [Live API](http://pressurenet.cumulonimbus.ca/livestream/). Of course, that project is [open source](https://github.com/JacobSheehy/pressureNETAnalysis) too.

Installation
--------------

Include this SDK in your Android project:

1. In Eclipse, open the Project Properties of your existing application. 
2. Open the Java Build Path page, then the Source tab. 
3. Use the Link Source feature to include the 'src' directory of this library. Rename the new folder to something like 'srk-cbsdk'.

Usage
--------
All interaction with the pressureNET SDK will be done through the CbService class. The only necessary step is to start the service by creating an Intent and calling startService:

    Intent intent  = new Intent(getApplicationContext(), CbService.class);
    startService(intent);

Add the service to your AndroidManifest.xml file inside the <application> tags:

    <service
        android:name="ca.cumulonimbus.pressurenetsdk.CbService"
        android:enabled="true" >
            <intent-filter>
                <action android:name="ca.cumulonimbus.pressurenetsdk.ACTION_SEND_MEASUREMENT" />
            </intent-filter>
    </service>

The final element required is to add the following <receiver> tag, which can go right below the <service> tag:

    <receiver
        android:name="ca.cumulonimbus.pressurenetsdk.CbAlarm"
        android:process=":remote" >
        <intent-filter>
            <action android:name="ca.cumulonimbus.pressurenetsdk.START_ALARM" />
        </intent-filter>
    </receiver> 

Communication
--------------------

Communication between an app and the CbService is done with Messages. Here's an example of asking the SDK for the total number of stored observations and then capturing the answer:


About
--------

This SDK is developed by [Cumulonimbus](http://cumulonimbus.ca) as part of the pressureNET project. Our primary goal is to improve short-term, local weather forecasting by dramatically increasing the data inputs available to weather models.
