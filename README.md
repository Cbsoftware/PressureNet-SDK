pressureNET SDK
==============

The pressureNET SDK is an Android library project that enables simple atmosphere sensor data collection and transmission to researchers. This SDK was created by [Cumulonimbus](http://cumulonimbus.ca) for use in [pressureNET](https://play.google.com/store/apps/details?id=ca.cumulonimbus.barometernetwork) and third-party Android apps. The code is open source and released under the MIT license. 

This SDK is intended for data collection. We have also built a pressureNET API for data dissemination that is actively used by scientists and researchers. We provide an explanation and documentation for the [Live API](http://pressurenet.io/developers/). Of course, that project is [open source](https://github.com/CbSoftware/pressureNET-server) too.

Installation
--------------

Include this SDK in your Android project:

1. In Eclipse, open the Project Properties of your existing application. 
2. Open the Java Build Path page, then the Source tab. 
3. Use the Link Source feature to include the 'src' directory of this library. Rename the new folder to something like 'srk-cbsdk'.

Usage
--------

There are two usage paradigms: 

1. Start the SDK Service and let it manage periodic data collection. 
2. Periodically send an Intent to the SDK to collect and send a single measurement at a time.

If your app already periodically runs in the background, it might make sense to pick option 2 rather than have two background services running. Otherwise, choose option 1 and let pressureNET do all the work. Simple steps to get started with the SDK are presented here. Full documentation is in the docs/ folder. We also provide a [full Example project](https://github.com/Cbsoftware/pressureNET-SDK-Example) that uses the SDK and simplified code to help you get started.

Option 1: Start the service and let it run

All interaction with the pressureNET SDK will be done through the CbService class. Start the service by creating an Intent and calling startService:

    Intent intent  = new Intent(getApplicationContext(), CbService.class);
    startService(intent);

Add the service to your AndroidManifest.xml file inside the <application> tags:

    <service
        android:name="ca.cumulonimbus.pressurenetsdk.CbService"
        android:enabled="true" >
            <intent-filter>
                <action android:name="ca.cumulonimbus.pressurenetsdk.ACTION_SEND_MEASUREMENT" />
                <action android:name="ca.cumulonimbus.pressurenetsdk.ACTION_REGISTER" />
            </intent-filter>
    </service>

The final element required is to add the following <receiver> tag, which can go right below the &lt;service&gt; tag:

    <receiver
        android:name="ca.cumulonimbus.pressurenetsdk.CbAlarm"
        android:process=":remote" >
        <intent-filter>
            <action android:name="ca.cumulonimbus.pressurenetsdk.START_ALARM" />
        </intent-filter>
    </receiver> 

And that's it! By adding those elements to your manifest and calling startService, the pressureNET SDK will run in the background and send data. For more control, your app can communicate with the SDK.

Option 2: Manually, periodically send Intents

You'll use the same Service tag as above in your AndroidManifest.xml:

    <service
        android:name="ca.cumulonimbus.pressurenetsdk.CbService"
        android:enabled="true" >
            <intent-filter>
                <action android:name="ca.cumulonimbus.pressurenetsdk.ACTION_SEND_MEASUREMENT" />
                <action android:name="ca.cumulonimbus.pressurenetsdk.ACTION_REGISTER" />
            </intent-filter>
    </service>

To send an intent directing the pressureNET SDK to send a single measurement, do something like this: 

    Intent intent = new Intent(getApplicationContext(), CbService.class);
    intent.setAction(CbService.ACTION_SEND_MEASUREMENT);
    startService(intent);

And that's it! The pressureNET SDK will collect and send (or bufffer) a single atmospheric pressure measurement.

Communication
--------------------

Communication between your app and the CbService is done by binding to the Service and passing Messages. For full documentation, please see Documentation.md and the docs/ folder, which contain descriptions of available communication messages as well as Javadoc comments.


About
--------

This SDK is developed by [Cumulonimbus](http://cumulonimbus.ca) as part of the [pressureNET](https://github.com/Cbsoftware/pressureNET) project. Our primary goal is to improve weather forecasting by dramatically increasing the data inputs available to weather models. We built this SDK so you can help this effort by contributing data! Please email us with any questions: software@cumulonimbus.ca
