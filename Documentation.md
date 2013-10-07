pressureNET SDK Documentation
==============

The [pressureNET SDK](https://github.com/Cbsoftware/pressureNET-SDK) is an Android library project that enables simple atmosphere sensor data collection and transmission to researchers. Please see README.md for an overview. This document details how to use the pressureNET SDK using our [Example Android project](https://github.com/Cbsoftware/pressureNET-SDK-Example).

Installation
========

This documentation assumes that you are using Eclipse with the Android Development Tools plugin (the Google-provided ADT or a standard Eclipse installation with ADT plugin will work fine). 

1. Download the most recent stable SDK source code from GitHub using
   git clone https://github.com/Cbsoftware/pressureNET-SDK
You may also choose to download our Example project, which this documentation uses for inline code examples and descriptions. You can get it with:
   git clone https://github.com/Cbsoftware/pressureNET-SDK-Example
2. In Eclipse, use the Import feature (File -> Import) to import the SDK project. 
[screenshot]
3. Link the Source of the SDK to your existing Android project by right-clicking on your project and selecting Properties. In the Properties dialog, on the left select Java Build Path and then click Link Source on the right.
[screenshot]
4. In the Link Source dialog, browse to the pressureNET-SDK/src directory and select it. Give it a name other than ‘src’ so as not to conflict with your existing projects.
[screenshot]
5. Congratulations! The pressureNET SDK is now imported into Eclipse and connected to your Android app. Before continuing on to the Usage section to learn how to use the SDK, ensure that everything builds fine - there should be no errors.

Usage
=====

You must reference the CbService class in your project’s AndroidManifest.xml in order to use it. Inside the <application> element, add a reference like this:

    <service
        android:name="ca.cumulonimbus.pressurenetsdk.CbService"
        android:enabled="true" >
            <intent-filter>
                <action android:name="ca.cumulonimbus.pressurenetsdk.ACTION_SEND_MEASUREMENT" />
            </intent-filter>
    </service>
    <receiver
        android:name="ca.cumulonimbus.pressurenetsdk.CbAlarm"
        android:process=":remote" >
        <intent-filter>
            <action android:name="ca.cumulonimbus.pressurenetsdk.START_ALARM" />
        </intent-filter>
    </receiver> 

The <service> tag identifies our service to the Android OS, and the <receiver> lets pressureNET receive the alarm signals it sends in the background, enabling auto-submit. 

Then, to start the service, use the following code to create an Intent and start the Service.

    Intent intent  = new Intent(getApplicationContext(), CbService.class);
    startService(intent);

Features of the SDK include the ability to view and change settings, start and stop the service, access saved data, access the live data set and other useful methods. Simple examples are shown here, with sample source code in the [SDK Example app](github.com/CbSoftware/pressureNET-SDK-Example).

Starting and Stopping
----------------------------

Starting CbService

To initialize the service and have it start with updated settings, create an Intent and start the service with a call to Android’s startService method. This will also initiate the Service and allow it to begin collecting and submitting data, if the settings allow.

	Intent serviceIntent;
	private void startCbService() {
		try {
			serviceIntent = new Intent(this, CbService.class);
			startService(serviceIntent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

Stopping CbService

	private void stopCbService() {
		try {
			stopService(serviceIntent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

Communication
===========

While pressureNET will run in the background with just the above commands, it's likely that you'll want to communicate with it to change settings, behaviour, and to query the data it has collected. Once your app is bound to the service, you can send Messages and objects to it and listen for responses.

First, bind to the service:

    bindService(new Intent(getApplicationContext(), CbService.class), mConnection, Context.BIND_AUTO_CREATE);

This can take time, so wait until your ServiceConnection object tells you it's bound (see the [Example source code](https://github.com/Cbsoftware/pressureNET-SDK-Example/blob/master/src/ca/cumulonimbus/pressurenetsdkexample/MainActivity.java) for clarity on this).

Now your app and the SDK can communicate. As an example, let's ask for the list of measurements the SDK has recorded. To request the stored readings, build a simple CbApiCall object and send it with message CbService.MSG_GET_LOCAL_RECENTS:
	
    CbApiCall apiCall = buildApiCall(); // Set a latitude and longitude range, along with a time range.
    Message msg = Message.obtain(null, CbService.MSG_GET_LOCAL_RECENTS, apiCall );
    try {
        msg.replyTo = mMessenger;
        mService.send(msg);
    } catch (RemoteException e) {
        System.out.println("Remote exception: " + e.getMessage());
    }
    
To read the result, build an IncomingHandler that looks something like this: 

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case CbService.MSG_LOCAL_RECENTS:
                ArrayList<CbObservation> obsList = (ArrayList<CbObservation>) msg.obj;
                // Do something with the ArryaList!
				break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    
A full list of available communication messages follows.

Settings
========

The pressureNET SDK offers a few settings that allow you to customize its behavior. To receive the current settings, send a CbService.MSG_GET_SETTINGS message. You will receive a CbSettingsHandler object back, which you can then read and modify, before saving the Settings with MSG_SET_SETTINGS. The available settings are documented here.


