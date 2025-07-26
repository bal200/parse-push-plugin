package github.taivo.parsepushplugin;

import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.lang.Exception;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import com.parse.Parse;
import com.parse.ParsePush;
import com.parse.ParseInstallation;
import com.parse.ParseGeoPoint;
import com.parse.SaveCallback;
import com.parse.ParseException;

import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class ParsePushPlugin extends CordovaPlugin {
  private static final String ACTION_GET_INSTALLATION_ID = "getInstallationId";
  private static final String ACTION_GET_INSTALLATION_OBJECT_ID = "getInstallationObjectId";
  private static final String ACTION_GET_SUBSCRIPTIONS = "getSubscriptions";
  private static final String ACTION_SUBSCRIBE = "subscribe";
  private static final String ACTION_UNSUBSCRIBE = "unsubscribe";
  private static final String ACTION_REGISTER_CALLBACK = "registerCallback";
  private static final String ACTION_REGISTER_FOR_PN = "register";
  public static final String ACTION_RESET_BADGE = "resetBadge";

  private static CallbackContext gEventCallback = null;
  private static Queue<PluginResult> pnQueue = new LinkedList();

  private static CordovaWebView gWebView;
  private static boolean gForeground = false;
  private static boolean helperPause = false;

  public static final String LOGTAG = "ParsePushPlugin";

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals(ACTION_REGISTER_CALLBACK)) {
      gEventCallback = callbackContext;

      if (!pnQueue.isEmpty()) {
        flushPNQueue();
      }
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
    if (action.equals(ACTION_RESET_BADGE)) {
      ParsePushPluginReceiver.resetBadge(this.cordova.getActivity().getApplicationContext());
      return true;
    }
    if (action.equals(ACTION_REGISTER_FOR_PN)) {
      this.registerDeviceForPN(callbackContext);
      return true;
    }
    if (action.equals("getLocation")) {
      this.getLocation(callbackContext);
      return true;
    }
    if (action.equals("setLocation")) {
      this.setLocation(args.getDouble(0), args.getDouble(1), callbackContext);
      return true;
    }
    if (action.equals("getDeviceToken")) {
      this.getDeviceToken(callbackContext);
      return true;
    }
    if (action.equals("getDeviceTokenFromFirebase")) {
      this.getDeviceTokenFromFirebase(callbackContext);
      return true;
    }
    return false;
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

  private void getLocation(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        ParseGeoPoint location = ParseInstallation.getCurrentInstallation().getParseGeoPoint("location");
        JSONObject obj = new JSONObject();
        try {
          obj.put("latitude", location.getLatitude());
          obj.put("longitude", location.getLongitude());
          callbackContext.success(obj);

        } catch(JSONException ex) {
          callbackContext.error(ex.toString());
        }
      }
    });
  }

  private void setLocation(double lat, double lng, final CallbackContext callbackContext) {
    ParseGeoPoint location = new ParseGeoPoint(lat, lng);
    ParseInstallation installation = ParseInstallation.getCurrentInstallation();
    installation.put("location", location);
    installation.saveInBackground(new SaveCallback() {
      @Override
      public void done(ParseException ex) {
        if (null != ex) {
          Log.e(LOGTAG, ex.toString());
          callbackContext.error(ex.toString());
        } else {
          Log.d(LOGTAG, "Location saved to Installation.");
          callbackContext.success();
        }
      }
    });
  }

  private void getDeviceToken(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        String deviceToken = ParseInstallation.getCurrentInstallation().getString("deviceToken");
        callbackContext.success(deviceToken);
      }
    });
  }

  private void getDeviceTokenFromFirebase(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
          @Override
          public void onComplete(@NonNull Task<String> task) {
            if(!task.isSuccessful()){
                Log.e(LOGTAG, "FAILED to get the deviceToken from Firebase.");
                callbackContext.error("FAILED to get the deviceToken from Firebase.");
                return;
            }
            String deviceToken = task.getResult();

            ParseInstallation installation = ParseInstallation.getCurrentInstallation();
            installation.setDeviceToken(deviceToken);
            installation.setPushType("gcm");
            installation.saveInBackground(new SaveCallback() {
              @Override
              public void done(ParseException ex) {
                if (null != ex) {
                  Log.e(LOGTAG, ex.toString());
                  callbackContext.error(ex.toString());
                } else {
                  Log.d(LOGTAG, "Got deviceToken from Firebase and saved it to the installation.");
                  callbackContext.success(deviceToken);
                }
              }
            });
          }
        });
      }
    });
  }

  private void getSubscriptions(final CallbackContext callbackContext) {
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        List<String> subscriptions = ParseInstallation.getCurrentInstallation().getList("channels");
        JSONArray subscriptionsArray = new JSONArray();
        if (subscriptions != null) {
          subscriptionsArray = new JSONArray(subscriptions);
        }
        callbackContext.success(subscriptionsArray);
      }
    });
  }

  private void subscribe(final String channel, final CallbackContext callbackContext) {
    ParsePush.subscribeInBackground(channel);
    callbackContext.success();
  }

  private void unsubscribe(final String channel, final CallbackContext callbackContext) {
    ParsePush.unsubscribeInBackground(channel);
    callbackContext.success();
  }

  private CallbackContext registerPNCallbackContext = null;

  private void registerDeviceForPN(final CallbackContext callbackContext) {
    /* First, we'll double check we have a deviceToken.  If not, get it from the Firebase SDK */
    String deviceToken = ParseInstallation.getCurrentInstallation().getString("deviceToken");
    if (deviceToken == null) {
      FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
        @Override
        public void onComplete(@NonNull Task<String> task) {
          if(task.isSuccessful()){
            String deviceToken = task.getResult();
            ParseInstallation installation = ParseInstallation.getCurrentInstallation();
            installation.setDeviceToken(deviceToken);
            installation.setPushType("gcm");
            installation.saveInBackground();
            Log.d(LOGTAG, "Got deviceToken from Firebase and saved it to the installation.");
          }else {
            Log.e(LOGTAG, "FAILED to get the deviceToken from Firebase.");
            callbackContext.error("FAILED to get the deviceToken from Firebase.");
          }
          requestNotificationPermission(callbackContext);
        }
      });
    } else {
      requestNotificationPermission(callbackContext);
    }
  }
  private void requestNotificationPermission(final CallbackContext callbackContext) {
    // This is only necessary for API level >= 33 (TIRAMISU)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (!cordova.hasPermission(android.Manifest.permission.POST_NOTIFICATIONS)) {
        registerPNCallbackContext = callbackContext;
        cordova.requestPermission(this, 0, android.Manifest.permission.POST_NOTIFICATIONS);
      } else {
        callbackContext.success();
      }
    } else {
      callbackContext.success();
    }
  }

  @Override
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
      throws JSONException {
    if (registerPNCallbackContext != null && permissions.length > 0
        && permissions[0].equals(android.Manifest.permission.POST_NOTIFICATIONS)) {
      if (grantResults[0] == 0) {
        registerPNCallbackContext.success(permissions[0]);
      } else {
        registerPNCallbackContext.error("POST_NOTIFICATIONS permission denied");
      }
      registerPNCallbackContext = null;
    }
  }

  /*
   * keep reusing the saved callback context to call the javascript PN handler
   */
  public static void jsCallback(JSONObject _json) {
    jsCallback(_json, "RECEIVE");
  }

  public static void jsCallback(JSONObject _json, String pushAction) {
    List<PluginResult> cbParams = new ArrayList<PluginResult>();
    cbParams.add(new PluginResult(PluginResult.Status.OK, _json));
    cbParams.add(new PluginResult(PluginResult.Status.OK, pushAction));
    //avoid blank
    PluginResult dataResult;
    if (pushAction.equals("OPEN")) {
      if (helperPause)
        dataResult = new PluginResult(PluginResult.Status.OK, _json);
      else
        dataResult = new PluginResult(PluginResult.Status.OK, cbParams);
    } else {
      dataResult = new PluginResult(PluginResult.Status.OK, _json);
    }
    dataResult.setKeepCallback(true);

    if (gEventCallback != null) {
      gEventCallback.sendPluginResult(dataResult);
    } else {
      //
      // save the incoming push payloads until gEventCallback is ready.
      // put a sensible limit on how queue size;
      if (pnQueue.size() < 10) {
        //pnQueue.add(new PNQueueItem(_json, pushAction));
        pnQueue.add(dataResult);
      }
    }
  }

  private static void flushPNQueue() {
    while (!pnQueue.isEmpty() && gEventCallback != null) {
      gEventCallback.sendPluginResult(pnQueue.remove());
    }
  }

  @Override
  protected void pluginInitialize() {
    gWebView = this.webView;
    gForeground = true;
  }

  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
    gForeground = false;
    helperPause = true;
  }

  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
    gForeground = true;
  }

  @Override
  public void onDestroy() {
    gWebView = null;
    gForeground = false;
    gEventCallback = null;
    helperPause = false;

    super.onDestroy();
  }

  public static boolean isInForeground() {
    return gForeground;
  }
}
