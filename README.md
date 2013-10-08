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

Simple steps to get started with the SDK are presented here. Full documentation is in the docs/ folder. We also provide a [full Example project](https://github.com/Cbsoftware/pressureNET-SDK-Example) that uses the SDK and simplified code to help you get started.

All interaction with the pressureNET SDK will be done through the CbService class. Start the service by creating an Intent and calling startService:

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

And that's it! By adding those elements to your manifest and calling startService, the pressureNET SDK will run in the background and send data. For more control, your app can communicate with the SDK.

Communication
--------------------

Communication between your app and the CbService is done by binding to the Service and passing Messages. For full documentation, please see Documentation.md and the docs/ folder, which contain descriptions of available communication messages as well as Javadoc comments.


About
--------

This SDK is developed by [Cumulonimbus](http://cumulonimbus.ca) as part of the pressureNET project. Our primary goal is to improve short-term, local weather forecasting by dramatically increasing the data inputs available to weather models. We built this SDK so you can help this effort by contributing data! Please email us with any questions: software@cumulonimbus.ca
