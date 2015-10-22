package org.apache.cordova.core;

import android.app.Application;
import android.util.Log;

//import java.util.Set;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.PushService;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.SaveCallback;

public class ParsePlugin extends CordovaPlugin {
  public static final String TAG = "ParsePlugin";
  public static final String ACTION_INITIALIZE = "initialize";
  public static final String ACTION_GET_INSTALLATION_ID = "getInstallationId";
  public static final String ACTION_GET_INSTALLATION_OBJECT_ID = "getInstallationObjectId";
  public static final String ACTION_GET_SUBSCRIPTIONS = "getSubscriptions";
  public static final String ACTION_SUBSCRIBE = "subscribe";
  public static final String ACTION_UNSUBSCRIBE = "unsubscribe";

  public static void initializeParseWithApplication(Application app) {
      String appId = getStringByKey(app, "parse_app_id");
      String clientKey = getStringByKey(app, "parse_client_key");
      Parse.enableLocalDatastore(app);
      Log.d(TAG, "Initializing with parse_app_id: " + appId + " and parse_client_key:" + clientKey);
      Parse.initialize(app, appId, clientKey);
  }

  private static String getStringByKey(Application app, String key) {
      int resourceId = app.getResources().getIdentifier(key, "string", app.getPackageName());
      return app.getString(resourceId);
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      if (action.equals(ACTION_INITIALIZE)) {
          this.initialize(callbackContext, args);
          return true;
      }
      if (action.equals(ACTION_GET_INSTALLATION_ID)) {
          this.getInstallationId(callbackContext);
          return true;
      }

      if (action.equals(ACTION_GET_INSTALLATION_OBJECT_ID)) {
          this.getInstallationObjectId(callbackContext);
          return true;
      }
      if (action.equals(ACTION_GET_SUBSCRIPTIONS)) {
          this.getSubscriptions(callbackContext);
          return true;
      }
      if (action.equals(ACTION_SUBSCRIBE)) {
          this.subscribe(args.getString(0), callbackContext);
          return true;
      }
      if (action.equals(ACTION_UNSUBSCRIBE)) {
          this.unsubscribe(args.getString(0), callbackContext);
          return true;
      }
      return false;
  }
  private void initialize(final CallbackContext callbackContext, final JSONArray args) {
      cordova.getThreadPool().execute(new Runnable() {
          public void run() {
              try {
                ParseInstallation.getCurrentInstallation().save();
                PushService.setDefaultPushCallback(cordova.getActivity(), cordova.getActivity().getClass());
                callbackContext.success();
              } catch(ParseException pe) {
                callbackContext.error("Parse error " + pe.getCode()
                    + ", Can't save Installation at this time.");
              }
          }
      });
  }

  private void getInstallationId(final CallbackContext callbackContext) {
      cordova.getThreadPool().execute(new Runnable() {
          public void run() {
              String installationId = ParseInstallation.getCurrentInstallation().getInstallationId();
              callbackContext.success(installationId);
          }
      });
  }

  private void getInstallationObjectId(final CallbackContext callbackContext) {
      cordova.getThreadPool().execute(new Runnable() {
          public void run() {
              String objectId = ParseInstallation.getCurrentInstallation().getObjectId();
              callbackContext.success(objectId);
          }
      });
  }

  private void getSubscriptions(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        //Set<String> subscriptions = PushService.getSubscriptions(cordova.getActivity());
        List<String> subscriptions = ParseInstallation.getCurrentInstallation().getList("channels");
        callbackContext.success(subscriptions.toString());
      }
    });
  }

  private void subscribe(final String channel, final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        ParsePush.subscribeInBackground(channel, new SaveCallback() {
          @Override
          public void done(ParseException e) {
            if (e == null) {
              callbackContext.success();
            }else{
              Log.e("com.parse.push", "failed to subscribe for push", e);
              callbackContext.error("Parse Error "+e.getCode());
            }
          }
        });
        //PushService.subscribe(cordova.getActivity(), channel, cordova.getActivity().getClass());
        //callbackContext.success();
      }
    });
  }

  private void unsubscribe(final String channel, final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        ParsePush.unsubscribeInBackground(channel, new SaveCallback() {
          @Override
          public void done(ParseException e) {
            if (e == null) {
              callbackContext.success();
            }else{
              Log.e("com.parse.push", "failed to unsubscribe for push", e);
              callbackContext.error("Parse Error "+e.getCode());
            }
          }
        });
        //PushService.unsubscribe(cordova.getActivity(), channel);
        //callbackContext.success();
      }
    });
  }

}

