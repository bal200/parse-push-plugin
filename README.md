Parse.Push Plugin
==============================

Parse.Push plugin for Cordova/Phonegap/ionic. Works for both hosted Parse.com and open source parse-server.


## Highlights

#### Update: This version now uses Firebase Cloud Message (FCM) instead of Google Cloud Messenging (GCM) on Android.

#### Works with [Parse.com](https://parse.com) and [parse-server](https://github.com/ParsePlatform/parse-server)

#### Supports Android 9 and iOs 8, 9, 10

#### Handles cold start out-of-the-box

#### Simple Setup

   1. `cordova plugin add https://github.com/bal200/parse-push-plugin`
   2. Set app id, server URL, and keys as `config.xml` `preference` tags.
   3. For Android: Get `google-services.json` from your Firebase console, and put in your projects root.
   4. Done! No fuss with Objective C, AndroidManifest, or Java

#### Simple API

   - **getInstallationId**( successCB, errorCB )
   - **getSubscriptions**( successCB, errorCB )
   - **subscribe**( channel, successCB, errorCB )
   - **unsubscribe**( channel, successCB, errorCB )
   - **resetBadge**( successCB, errorCB )
   - **register**( successCB, errorCB ) //optional, see [Advanced Configuration](#advanced-configuration)

#### Notification events

   Handle `openPN, receivePN, receivePN:customEvt` anywhere in your javascript code.

   ```javascript

   ParsePushPlugin.on('receivePN', function(pn){
	   console.log('yo i got this push notification:' + JSON.stringify(pn));
   });

   //
   // Use custom events to simulate separate communication channels using push notification.
   // Just set an 'event' key in the push payload made from your server. If you set {event: "x"},
   // you'll be able to catch it via "receivePN:x"
   //
   ParsePushPlugin.on('receivePN:chat', function(pn){
	   console.log('yo i can also use custom event to keep things like chat modularized');
   });
   ParsePushPlugin.on('receivePN:system-maintenance', function(pn){
	   console.log('yo, here is a system maintenance payload');
   });

   //
   // When you open a notification from the system tray, `openPN` is also triggered.
   // You can use it to do things like navigating to a different page or refreshing data.
   ParsePushPlugin.on('openPN', function(pn){
	   //you can do things like navigating to a different view here
	   console.log('Yo, I get this when the user taps open a notification from the tray');
   });

   ```

#### Multiple notifications

   *Android*: to prevent flooding the notification tray, this plugin retains only the last PN with the same `title` field. For messages without the `title` field, the application name is used. A count of unopened PNs is shown.

   You can override this feature, however, by adding the following to `config.xml`:

   ```xml
   <preference name="ParseMultiNotifications" value="true" />
   ```


#### Foreground vs. Background

   *Android*: Mimic the iOS behavior and create a notification in the system tray when app is off or in background. When app is in foreground, PN payloads are forwarded via the `receivePN` and `receivePN:customEvt` events.

   *iOS*: Forward the PN payload to javascript in foreground mode. When app inactive or in background, iOS holds PNs in the tray. Only when the user opens these PNs would we have access and forward them to javascript.


## Installation

#### Install Push Certificates on Server:

- Open Source parse-server
   1. Setup `parse-server`

      There are plenty of guides out there to help you get started on popular hosting services like Heroku and AWS. If you want to setup parse-server on your laptop [for local development, here's a quick-start guide](https://taivo.github.io/guides/parse-server-for-local-development).
   2. Once you have a working `parse-server`, generate your push credentials:

      - iOS
         1. Create SSL push certificate with Apple. You may find  [this tutorial useful](https://github.com/ParsePlatform/PushTutorial/tree/master/iOS). All steps prior to adding code to your iOS application are applicable.
         2. Place the `p12` certificate file from the previous step on your server.
      - Android
         1. Get the sender id (your project number) from your google developer console. It's a long integer.
         2. Enable GCM for your project on google developer console and generate a **server API key**.
   3. Update your `parse-server` configuration to use the push credentials. Here is an example:

      ```json
      {
         "appId": "MY_APP_ID",
         "masterKey": "SUPER_SECRET",
         "cloud": "./myCloudDir/main.js",
         "push": {
            "android":{
               "senderId": "SENDER_ID_AKA_PROJECT_NUMBER",
               "apiKey": "SERVER_API_KEY_FROM_GOOGLE_DEVELOPER_CONSOLE"
            },
            "ios":{
               "pfx": "my-push-certificate.p12",
               "bundleId": "com.company.myapp",
               "production": false
            }
         }
      }
      ```
   4. Restart your `parse-server` for the new settings to take effect.

- Hosted Parse.com
   - iOS
      1. Create SSL push certificate with Apple. You may find  [this tutorial useful](https://github.com/ParsePlatform/PushTutorial/tree/master/iOS). All steps prior to adding code to your iOS application are applicable.
      2. Use Parse Dashboard to upload the generated `p12` push certificate.
   - Android - no need for certificate setup. Parse.com uses its own push credentials.


#### Add Plugin

```bash
cordova plugin add https://github.com/taivo/parse-push-plugin

```

Create the following tags in `config.xml`:

   - For open source parse-server

   ```xml
   <preference name="ParseAppId" value="your-parse-app-id" />
   <preference name="ParseServerUrl" value="http://your-parse-server:1337/parse/" />

   <!-- If your parse-server config requires a client key, set this.
     If not, skip this preference -->
   <preference name="ParseClientKey" value="your-parse-client-key" />

   <!-- required for Android push notification
      To get your GCM sender ID, enable GCM for your Android project in the Google Developer Console.
      The sender id is your project number, and should be a large integer like 123427208255.
      This is the same "senderId" to be used in your parse-server push configuration.
   -->
   <preference name="ParseGcmSenderId" value="gcm-sender-id" />

   <!-- As standard, this plugin only shows the most recent PN in 
      the android notifications tray along with a count of unopened 
      PNs. If you would like to override this behaviour and show all 
      PNs in the tray, then add this preference. 
      If not, skip this preference -->
   <preference name="ParseMultiNotifications" value="true" />
   ```

   - For legacy Parse.com

   ```xml
   <preference name="ParseAppId" value="your-parse-app-id" />
   <preference name="ParseClientKey" value="your-parse-client-key" />

   <!-- Do not replace this string value. It must be "PARSE_DOT_COM"-->
   <preference name="ParseServerUrl" value="PARSE_DOT_COM" />

   <!-- As standard, this plugin only shows the most recent PN in 
      the android notifications tray along with a count of unopened
      PNs. If you would like to override this behaviour and show all
      PNs in the tray, then add this preference. 
      If not, skip this preference -->
   <preference name="ParseMultiNotifications" value="true" />
   ```

You're all set. The plugin takes care of initializing Parse platform using the `config.xml` preferences mentioned above. It also saves your installation to the database automatically.

To customize push notifications, initialize Parse platform yourself, or use your own `MainApplication.java` in Android, see the [Advanced Configuration](#advanced-configuration) section.


## Usage

When your app starts, ParsePushPlugin automatically obtains and stores necessary device tokens to your native `ParseInstallation`. It also registers a javascript callback that will be triggered when a push notification is opened or received.

#### Receiving push notifications

Anywhere in your code, you can set a listener for notification events using the ParsePushPlugin object.

```javascript
$ionicPlatform.ready(function(){
   if(window.ParsePushPlugin){
   	ParsePushPlugin.on('receivePN', function(pn){
   		alert('yo i got this push notification:' + JSON.stringify(pn));
   	});

   	//
   	//you can also listen to your own custom events
   	// Note: to push custom event, include 'event' key in your push payload,
      // e.g. {alert: "sup", event:'chat'}
   	ParsePushPlugin.on('receivePN:chat', chatEventHandler);
   	ParsePushPlugin.on('receivePN:serverMaintenance', serverMaintenanceHandler);

      //
      // When the app is off or in background, push notifications get added
      // to the notification tray. When a user open a notification, you
      // can catch it via openPN
      ParsePushPlugin.on('openPN', function(pn){
   		alert('a notification was opened:' + JSON.stringify(pn));
   	});
   }
});
```

#### Subscriptions and Installation Id

```javascript
$ionicPlatform.ready(function(){
   if(window.ParsePushPlugin){
      ParsePushPlugin.getInstallationId(function(id) {
         // note that the javascript client has its own installation id,
         // which is different from the device installation id.
          alert("device installationId: " + id);
      }, function(e) {
          alert('error');
      });

      ParsePushPlugin.getSubscriptions(function(subscriptions) {
          alert(subscriptions);
      }, function(e) {
          alert('error');
      });

      ParsePushPlugin.subscribe('SampleChannel', function(msg) {
          alert('OK');
      }, function(e) {
          alert('error');
      });

      ParsePushPlugin.unsubscribe('SampleChannel', function(msg) {
          alert('OK');
      }, function(e) {
          alert('error');
      });
   }
});
```

#### Navigate to a specific view when user opens a notification

If your app is already on (or in the background), you can simply perform page switching in javascript. Just add a `urlHash` field in your PN payload that contains either a url hash, i.e. #myhash, or a url parameter string, i.e. ?param1=a&param2=b. Then catch that field via the `openPN` event and go from there.

```javascript
ParsePushPlugin.on('openPN', function(pn){
	if(pn.urlHash){
		window.location.hash = hash;
	}
});
```

For cold start, you can also let your cordova app finish loading and use javascript to handle page switching. Carry out this type of page switching while the spashscreen is still visible for a better user experience.

Directly launching a non-default url via native code is also possible. Here are some hints on how to do that:

*Android*: If `urlHash` starts with "#" or "?", this plugin will pass it along as an extra in the android intent to launch your MainActivity. You can then launch the custom url in `MainActivity.onCreate` this way:

```java
@Override
public void onCreate(Bundle savedInstanceState)
{
    //
    // your code...
    //

    String urlHash = intent.hasExtra("urlHash") ? intent.getStringExtra("urlHash") : "";
    loadUrl(launchUrl + urlHash);
}
```

*iOS*: On cold start via notification, `didFinishLaunchingWithOptions` and this plugin's `didLaunchViaNotification`
have access to the payload. Those 2 functions are good starting points for launching custom url.


#### Silent Notifications

For Android, a silent notification can be sent by omitting the `title` and `alert` fields in the JSON payload. This means the push notification will not be shown in the system tray, but its JSON payload will still be delivered to your `receivePN` and `receivePN:customEvt` handlers.


Advanced Configuration
----------------------

#### Android:

The actual code that handles Parse platform initialization is in [ParsePushApplication.java](src/android/ParsePushApplication.java).

Android knows to use this class due to the attribute `android:name` in `<application>` in 'platforms/android/app/src/main/AndroidManifest.xml'.
To preserve your customizations, this plugin sets `android:name="github.taivo.parsepushplugin.ParsePushApplication"`  
if and only if `android:name` is not already defined. It does this during plugin installation. Similarly, when the plugin is
uninstalled, `android:name` will be removed only if its content matches `github.taivo.parsepushplugin.ParsePushApplication` exactly.

If you use your own Application class, don't forget to update `android:name` to point to it.

*Optional: Write your own MainApplication and/or initialize Parse yourself:* Look at [ParsePushApplication.java](src/android/ParsePushApplication.java).
The comments contain all the explanations and hints you will need. Mimic the code to write your own customized implementation.

*Optional: Customize background color for the push notification icon in Android Lollipop:* Go to your `platforms/android/res/values` folder and create a file named `colors.xml`. Paste the following content in it and replace the hex color value of the form `#AARRGGBB` to your liking.

   ```xml
	   <?xml version="1.0" encoding="utf-8"?>
      <resources>
         <color name="parse_push_icon_color">#ff112233</color>
      </resources>
   ```

You can provide a property `ParseNotificationIcon` in `config.xml` to provide a custom android notification icon for Android Lollipop and above like this:

```xml
    <preference name="ParseNotificationIcon" value="android_notification_icon" />
```

The icon has to be in folder `resources` in project root and with extension `.png`. The icon is then copied to `platforms/android/app/src/main/res/drawable/<PROVIDED-NAME>.png`. For details, how to design the icon have a look at https://clevertap.com/blog/fixing-notification-icon-for-android-lollipop-and-above/

#### iOS:

By default, `ParsePushPlugin` automatically registers your device for push notification on app startup. This means
your app will ask for push notification permission at the very beginning of the first app start. For UX reason,
you may want to delay asking the user for that permission until you absolutely need it. To do so,
add `<preference name="ParseAutoRegistration" value="false" />` to `config.xml`
and manually call `ParsePushPlugin.register(successCB, errorCB)` in your javascript.

If you want to completely customize your notification settings, use the method `didFinishLaunchingWithOptions`
in [AppDelegate+parsepush.m](src/ios/AppDelegate+parsepush.m) as a guide to modify the same method in
your `platforms/ios/ProjectName/Classes/AppDelegate.m`.

When you initialize Parse from your own `platforms/ios/ProjectName/Classes/AppDelegate.m`, this plugin will
skip it's version of Parse initialization and notification setup, that way it won't override your customization.


## Troubleshooting

#### General:

- Parse uses the term "client key" to specify a key for both Android and iOS. This is different from the Javascript key.
In the javascript portion of your Cordova/Phonegap/Ionic app, use the Javascript key. This has nothing to do with the plugin.
In the `config.xml` preference `ParseClientKey`, use the Android and iOS client key. Note that for open source [parse-server](https://github.com/ParsePlatform/parse-server), these keys are optional.

- For legacy [Parse.com](https://parse.com), the appropriate key is required depending on your client platform, e.g.,
Javascript, Android or iOS client, dotNet, REST. For open source [parse-server](https://github.com/ParsePlatform/parse-server),
a key is only required if you have configured your server with it. In the past, our Android users have
reported strange errors in their logcat relating to missing permission declarations. It turns out those error messages were red herrings.
The real problem involved a parse-server that required client keys and a missing `ParseClientKey` preference in `config.xml`.


#### Android:

- If you run into this error during build

   ```
   > Could not resolve all dependencies for configuration ':_debugCompile'.
      > Could not find any version that matches com.android.support:support-v4:+.
        Searched in the following locations:
            https://repo1.maven.org/maven2/com/android/support/support-v4/maven-metadata.xml
            https://repo1.maven.org/maven2/com/android/support/support-v4/
        Required by:
            :android:unspecified
   ```

   Update your android SDK installation to include android-extra:

   ```bash

   android update sdk --no-ui --filter extra

   ```


- Starting with the Parse Android SDK v1.10.1 update, your app may crash at start and the log says something about a missing method in OkHttpClient. Just update the cordova libs of your project
via `cordova platform update android`. If your previous cordova libs are old, you may run into further compilation errors that has to do with the new cordova libs setting your android target to be 22 or higher. Look at file `platforms/android/project.properties` and make sure that is
consistent with your `config.xml`

#### iOS:

This plugin takes advantage of the `cordova.exec` bridge. If calls to `cordova.exec` only gets triggered after pressing your device's Home button, try inspecting your Content-Security-Policy. Your `frame-src` must allow `gap:` because the cordova bridge on iOS works via Iframe.
