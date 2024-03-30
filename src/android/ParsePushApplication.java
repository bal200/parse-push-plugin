package github.taivo.parsepushplugin;

import android.app.Application;

import com.parse.Parse;
import com.parse.Parse.Configuration.Builder;
import com.parse.ParseInstallation;
import com.parse.SaveCallback;
import com.parse.ParseException;

import github.taivo.parsepushplugin.ParsePushConfigReader;
import github.taivo.parsepushplugin.ParsePushConfigException;

import android.util.Log;

/*
   Why is this Application subclass needed?
      - Cordova does not define an Application class, only Activity.
      - The android cold start sequence is: create Application -> ... --> handle push --> ... -> launch Activity,
      - Without configuring an Application class, the app would crash during push notification cold start because
         Parse.Push is not initialized before the "handle push" phase.

   How does Android know to use this subclass as the main application class?
      - In AndroidManifest.xml, the <application> class has an attribute "android:name" that points to your designated main application class.
      - This plugin automatically sets android:name during plugin installation IFF it doesn't exist.
      - If you write your own MainApplication class in your app package, be sure to manually set android:name="MainApplication"
      - If your MainApplication resides in a package other than your main app package, the full path must be specified,
         i.e., android:name="com.custom.package.MainApplication"
*/
public class ParsePushApplication extends Application {
  public static final String LOGTAG = "ParsePushApplication";

  @Override
  public void onCreate() {
    super.onCreate();

    try {
      // Other ways to call ParsePushReaderConfig:
      //
      // - Tell the reader to parse custom parameters, e.g., <preference name="CustomParam1" value="foo" />
      //   ParsePushConfigReader config = new ParsePushConfigReader(getApplicationContext(), null, new String[] {"CustomParam1", "CustomParam2"});
      //
      // - If you write your own MainApplication in your app package, just import com.yourpackage.R and skip detecting R.xml.config
      //   ParsePushConfigReader config = new ParsePushConfigReader(getApplicationContext(), R.xml.config, null);
      //

      // Simple config reading for opensource parse-server:
      // 1st null to detect R.xml.config resource id, 2nd null indicates no custom config param
      //ParsePushConfigReader config = new ParsePushConfigReader(getApplicationContext(), null, null);
      //
      //Parse.initialize(new Parse.Configuration.Builder(this)
      //   .applicationId(config.getAppId())
      //   .server(config.getServerUrl()) // The trailing slash is important, e.g., https://mydomain.com:1337/parse/
      //   .build()
      //);

      //
      // Support parse.com and opensource parse-server
      // 1st null to detect R.xml.config
      ParsePushConfigReader config = new ParsePushConfigReader(getApplicationContext(), null,
          new String[] { "ParseClientKey" });
      if (config.getServerUrl().equalsIgnoreCase("PARSE_DOT_COM")) {
        //
        //initialize for use with legacy parse.com
        //Parse.initialize(this, config.getAppId(), config.getClientKey());
      } else {
        Log.d(LOGTAG, "ServerUrl " + config.getServerUrl());
        Log.d(LOGTAG, "NOTE: The trailing slash is important, e.g., https://mydomain.com:1337/parse/");
        Log.d(LOGTAG, "NOTE: Set the clientKey if your server requires it, otherwise it can be null");
        //
        // initialize for use with opensource parse-server
        Parse.initialize(new Parse.Configuration.Builder(this)
            .applicationId( config.getAppId() )
            .server( config.getServerUrl() )
            .clientKey( config.getClientKey() )
            .build()
        );
      }

      Log.d(LOGTAG, "Saving Installation in background");
      //
      // save installation. Parse.Push will need this to push to the correct device
      ParseInstallation installation = ParseInstallation.getCurrentInstallation();
      installation.saveInBackground(new SaveCallback() {
        @Override
        public void done(ParseException ex) {
          if (null != ex) {
            Log.e(LOGTAG, ex.toString());
          } else {
            Log.d(LOGTAG, "Installation saved");
          }
        }
      });

    } catch (ParsePushConfigException ex) {
      Log.e(LOGTAG, ex.toString());
    }
  }
}
