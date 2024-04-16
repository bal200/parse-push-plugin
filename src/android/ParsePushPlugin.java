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

import android.util.Log;

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

  private void registerDeviceForPN(final CallbackContext callbackContext) {
    //
    // just a stub to keep API consistent with iOS.
    // Device registration is automatically done by the Parse SDK for Android
    callbackContext.success();
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
