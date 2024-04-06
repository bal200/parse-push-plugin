package github.taivo.parsepushplugin;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

import github.taivo.parsepushplugin.ParsePushConfigException;

import android.util.Log;

public final class ParsePushConfigReader {
  //
  // Extract relevant Parse.Push settings from config.xml
  //
  public static final String LOGTAG = "ParsePushConfigReader";

  private static final String preferenceTag = "preference"; //<preference name="ParseAppId" value="foo" />

  //
  // appId and serverUrl are required. They get special treatment, including their own get functions
  //
  private static final String parseAppIdKey = "ParseAppId";
  private static final String parseServerUrlKey = "ParseServerUrl";

  // ParseClientKey is not required by parse server, but can be required by some environments
  private static final String parseClientKeyKey = "ParseClientKey";

  private List<String> supportedKeys = new ArrayList(Arrays.asList(parseAppIdKey, parseServerUrlKey));
  private Map<String, String> configs;

  private static int getConfigXmlResourceId(Context context) {
    return context.getResources().getIdentifier("config", "xml", context.getPackageName());
  }

  public ParsePushConfigReader(Context context, Integer configXmlResourceId, String[] moreSupportedKeys)
      throws ParsePushConfigException {
    int configXmlResId = (configXmlResourceId == null ? getConfigXmlResourceId(context) : configXmlResourceId);

    if (moreSupportedKeys != null) {
      supportedKeys.addAll(Arrays.asList(moreSupportedKeys));
    }

    configs = loadConfigsFromXml(context.getResources(), configXmlResId);
    if (!hasRequiredParams()) {
      throw new ParsePushConfigException(
          parseAppIdKey + " or " + parseServerUrlKey + " is missing! Please set them in config.xml");
    }
  }

  public String getAppId() {
    return configs.get(parseAppIdKey);
  }

  public String getServerUrl() {
    return configs.get(parseServerUrlKey);
  }

  public String getClientKey() {
    return configs.get(parseClientKeyKey);
  }

  public Boolean getParseAlwaysTrayNotification() {
    String str = configs.get("ParseAlwaysTrayNotification");
    return (str != null && !str.isEmpty() && str.equals("true"));
  }

  public String get(String key) {
    return configs.get(key);
  }

  public Boolean hasRequiredParams() {
    return !(getAppId() == null || getServerUrl() == null || getAppId().isEmpty() || getServerUrl().isEmpty());
  }

  private String matchSupportedKeyName(String testKey) {
    //
    // If key matches, return the version with correct casing.
    // If not, return null.
    // O(n) here is okay because this is a short list of just a few items
    for (String realKey : supportedKeys) {
      if (realKey.equalsIgnoreCase(testKey)) {
        return realKey;
      }
    }
    return null;
  }

  private Map loadConfigsFromXml(Resources res, int configXmlResourceId) {
    XmlResourceParser xrp = res.getXml(configXmlResourceId);

    Map configs = new HashMap();

    //
    // walk the config.xml tree and save all <preference> tags related to Parse.Push
    //
    try {
      xrp.next();
      while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
        if (preferenceTag.equals(xrp.getName())) {
          String key = matchSupportedKeyName(xrp.getAttributeValue(null, "name"));
          if (key != null) {
            configs.put(key, xrp.getAttributeValue(null, "value"));
          }
        }
        xrp.next();
      }
    } catch (XmlPullParserException ex) {
      Log.e(LOGTAG, ex.toString());
    } catch (IOException ex) {
      Log.e(LOGTAG, ex.toString());
    }

    return configs;
  }
}
