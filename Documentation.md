PressureNet SDK Documentation
==============

The [PressureNet SDK](https://github.com/Cbsoftware/pressureNET-SDK) is an Android library project that enables simple atmosphere sensor data collection and transmission to researchers. Please see README.md for an overview. This document details how to use the PressureNet SDK using our [Example Android project](https://github.com/Cbsoftware/pressureNET-SDK-Example).

Installation
------------

This documentation assumes that you are using Eclipse with the Android Development Tools plugin (the Google-provided ADT or a standard Eclipse installation with ADT plugin will work fine). 

1. Download the most recent stable SDK source code from GitHub using
   git clone https://github.com/Cbsoftware/PressureNet-SDK
You may also choose to download our Example project, which this documentation uses for inline code examples and descriptions. You can get it with:
   git clone https://github.com/Cbsoftware/PressureNet-SDK-Example
2. In Eclipse, use the Import feature (File -> Import) to import the SDK project. 
3. Link the Source of the SDK to your existing Android project by right-clicking on your project and selecting Properties. In the Properties dialog, on the left select Java Build Path and then click Link Source on the right.
4. In the Link Source dialog, browse to the PressureNet-SDK/src directory and select it. Give it a name other than ‘src’ so as not to conflict with your existing projects.
5. Congratulations! The PressureNet SDK is now imported into Eclipse and connected to your Android app. Before continuing on to the Usage section to learn how to use the SDK, ensure that everything builds fine - there should be no errors.

Usage
-----

The PressureNet SDK sends and receives data from our servers. In order to use the SDK, please sign up for an API key and get a PressureNet developer account at https://pressurenet.io/. You'll receive an API key by email, which you can then put into CbConfiguration to send and receive data. 

You must reference the CbService class in your project's AndroidManifest.xml in order to use it. Inside the <application> element, add a reference like this:

    <service
        android:name="ca.cumulonimbus.pressurenetsdk.CbService"
        android:enabled="true" >
            <intent-filter>
                <action android:name="ca.cumulonimbus.pressurenetsdk.ACTION_SEND_MEASUREMENT" />
                <action android:name="ca.cumulonimbus.pressurenetsdk.ACTION_REGISTER" />
            </intent-filter>
    </service>
    <receiver
        android:name="ca.cumulonimbus.pressurenetsdk.CbAlarm"
        android:process=":remote" >
        <intent-filter>
            <action android:name="ca.cumulonimbus.pressurenetsdk.START_ALARM" />
        </intent-filter>
    </receiver> 

The <service> tag identifies our service to the Android OS, and the <receiver> lets PressureNet receive the alarm signals it sends in the background, enabling auto-submit. 

Then, to start the service, use the following code to create an Intent and start the Service. This will tell the service to run automatically in the background and periodically collect and send data.

    Intent intent  = new Intent(getApplicationContext(), CbService.class);
    startService(intent);

If you prefer to manage the timing yourself and not have PressureNet service running in the background, you can periodically send an Intent directing the SDK to send just a single measurement:

    Intent intent = new Intent(getApplicationContext(), CbService.class);
    intent.setAction(CbService.ACTION_SEND_MEASUREMENT);
    startService(intent);

Features of the SDK include the ability to view and change settings, start and stop the service, access saved data, access the live data set and other useful methods. Simple examples are shown here, with sample source code in the [SDK Example app](github.com/CbSoftware/PressureNet-SDK-Example).

Starting and Stopping
----------------------------

Starting CbService:

To initialize the service and have it start with updated settings, create an Intent and start the service with a call to Android's startService method. This will also initiate the Service and allow it to begin collecting and submitting data, if the settings allow.

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
-------------

While PressureNet will run in the background with just the above commands, it's likely that you'll want to communicate with it to change settings, behaviour, and to query the data it has collected. Once your app is bound to the service, you can send Messages and objects to it and listen for responses. 

First, bind to the service:

    bindService(new Intent(getApplicationContext(), CbService.class), mConnection, Context.BIND_AUTO_CREATE);

This can take time, so wait until your ServiceConnection object tells you it's bound (see the [Example source code](https://github.com/Cbsoftware/PressureNet-SDK-Example/blob/master/src/ca/cumulonimbus/pressurenetsdkexample/MainActivity.java) for clarity on this).

Now your app and the SDK can communicate. As an example, let's ask for the list of measurements the SDK has recorded. To request the stored readings, build a simple CbApiCall object and send it with message CbService.MSG_GET_LOCAL_RECENTS:

    private void askForRecents() {
        CbApiCall apiCall = buildApiCall(); // Set a latitude and longitude range, along with a time range.
        Message msg = Message.obtain(null, CbService.MSG_GET_LOCAL_RECENTS, apiCall );
        try {
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            System.out.println("Remote exception: " + e.getMessage());
        }
    }
    
To read the result, build an IncomingHandler that looks something like this: 

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case CbService.MSG_LOCAL_RECENTS:
                ArrayList<CbObservation> obsList = (ArrayList<CbObservation>) msg.obj;
                // Do something with the ArrayList!
                break;
            // ... Handle more cases here for different messages 
            default:
                super.handleMessage(msg);
            }
        }
    }

and instantiate it like this: 

    private Messenger mMessenger = new Messenger(new IncomingHandler());
    
A list of available communication messages, with examples, follows:

**Ask for best location**

The SDK has a series of methods to determine an accurate user location. You can quickly ask for this location: 

    private void askForBestLocation() {
        Message msg = Message.obtain(null, CbService.MSG_GET_BEST_LOCATION, 0, 0);
        try {
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            System.out.println("Remote exception: " + e.getMessage());
        }
    }

**Receive best location**

The SDK returns an Android Location object. All messages are received in your IncomingHandler, so to handle different messages just add a new case statement: 

    // ...
    case CbService.MSG_BEST_LOCATION:
        Location bestLocation = (Location) msg.obj;
        if (bestLocation != null) {
            // Use the bestLocation object
        } else {
            log("location returned null");
        }
        break;
    // ...

**Ask for settings**

    private void askForSettings() {
        Message msg = Message.obtain(null, CbService.MSG_GET_SETTINGS, 0, 0);
        try {
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            System.out.println("Remote exception: " + e.getMessage());
        }
    }

**Receive settings**

    // ...
    case CbService.MSG_SETTINGS:
        CbSettingsHandler settings = (CbSettingsHandler) msg.obj;
        if (settings != null) {
            // Use the CbSettingsHandler object
        } else {
            log("location returned null");
        }
        break;
    // ...


**Save new settings**

    private void saveSettings(CbSettingsHandler newSettings) {
        Message msg = Message.obtain(null, CbService.MSG_SET_SETTINGS, newSettings);
        try {
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            System.out.println("Remote exception: " + e.getMessage());
        }
    }

**Ask for locally recorded measurements**

    private void askForLocalRecents() {
        CbApiCall apiCall = buildApiCall(); 
        Message msg = Message.obtain(null, CbService.MSG_GET_LOCAL_RECENTS, apiCall );
        try {
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            System.out.println("Remote exception: " + e.getMessage());
        }
    }

    // Set a latitude and longitude range, along with a time range.
    private CbApiCall buildApiCall() {
        long timeNow = System.currentTimeMillis();
        CbApiCall apiCall = new CbApiCall();
        apiCall.setMinLat(-90);
        apiCall.setMaxLat(90);
        apiCall.setMinLon(-180);
        apiCall.setMaxLon(180);
        apiCall.setStartTime(timeNow - (60 * 60 * 1000); // one hour ago
        apiCall.setEndTime(timeNow);
        return apiCall;
    }

**Receive locally recorded measurements**

    case CbService.MSG_LOCAL_RECENTS:
        ArrayList<CbObservation> obsList = (ArrayList<CbObservation>) msg.obj;
        break;

**Make remote API call**

    private void makeAPICall(CbApiCall apiCall) {
        Message msg = Message.obtain(null, CbService.MSG_MAKE_API_CALL, apiCall );
        try {
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            System.out.println("Remote exception: " + e.getMessage());
        }
    }

**Listen for API call results**

The SDK will return the message MSG_API_RESULT_COUNT when the call is finished rather than returning the results directly. You can use this message to know when the call has completed and to see how many results were obtained, but there are different messages to access those results (MSG_GET_API_RECENTS and MSG_API_RECENTS; see below).

     case CbService.MSG_API_RESULT_COUNT:
        int count = msg.arg1;
        System.out.println(count + " results returned");
        break;

**Load API call results**

To load the results, send the message MSG_GET_API_RECENTS with the came CbApiCall object you used to make the initial API call.

    private void askForAPIRecents() {
        CbApiCall apiCall = buildApiCall(); 
        Message msg = Message.obtain(null, CbService.MSG_GET_API_RECENTS, apiCall );
        try {
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            System.out.println("Remote exception: " + e.getMessage());
        }
    }

The results will be returned via MSG_API_RECENTS:

    case CbService.MSG_API_RECENTS:
        ArrayList<CbObservation> apiRecents = (ArrayList<CbObservation>) msg.obj;
        // Do something with apiRecents
        break;

**Clear user data cache**

User data is stored on the device for fast access, but you may want to clear it at some point. Note that there is not currently a supported method of retrieving the user's data from the servers, as this is returned in aggregate for users in a region, not for a specific user. Be sure that if you clear the user cache, you have retrieved the data already if you want to keep it.

    private void clearLocalCache() {
        Message msg = Message.obtain(null, CbService.MSG_CLEAR_LOCAL_CACHE, 0, 0);
        try {
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) { 
            // ...
        }
    }

**Clear API data cache**

The SDK does not store API data for more than 12 hours, so generally this archive does not grow excessively large. However, if you do want to clear it we offer this Message:


    private void clearLocalCache() {
        Message msg = Message.obtain(null, CbService.MSG_CLEAR_API_CACHE, 0, 0);
        try {
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) { 
            // ...
        }
    }

Settings
--------

The PressureNet SDK offers settings that allow you to customize its behavior. To receive the current settings, send a *CbService.MSG_GET_SETTINGS* message. You will receive a CbSettingsHandler object back, which you can then read and modify, before saving the Settings with *CbService.MSG_SET_SETTINGS*. The available settings are documented here. 

**Data Collection Frequency**

This specifies how frequently PressureNet will run in the background to collect data. The values are stored in milliseconds, and the SDK uses Android's AlarmManager to set these alarms. There is no guarantee that data will be collected on schedule, as various resources may be unavailable (such as location). When the network is not available, data is collected on schedule and stored for transmission the next time the alarm fires with an active network connection. The default value is 10 minutes (600000ms).

**Toggle auto-submit**

Choose whether to automatically submit data or not. This setting does not affect data collection, only data transmission. The default value is on.

**Sharing preference**

Since our data includes user locations, we provide a variety of sharing options:

- Nobody
 - No data is sent from the device
- Cumulonimbus (Us)
 - Only Cumulonimbus will see this data; it is not available in the API
- Us and Researchers
 - Only Academic Researchers are allowed to access this data; it will not be returned in your API call unless we have confirmed you are a researcher.
- Us, Researchers and Forecasters
 - Both researchers and private forecasters are allowed to access this data
- Public
 - Everyone can access this data. 

The default value is "Us, Researchers and Forecasters"

**Notification toggle**

The PressureNet SDK can send a message (*CbService.MSG_CHANGE_NOTIFICATION*) to your app when it detects a change in pressure trend. The default value is off.

**GPS toggle**

If GPS is available and active on the device, PressureNet can use it for location purposes but it can also be ignored (to save battery, for example). The default value is on.

**Only-when-charging toggle**

If this is set (default is off), PressureNet will only run when the devices is plugged in and charging / fully charged.
