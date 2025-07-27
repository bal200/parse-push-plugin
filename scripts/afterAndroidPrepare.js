module.exports = function (context) {
  var fs = require('fs');
  var path = require('path');
  var ET = require('elementtree');
  var ConfigFile = require("cordova-common").ConfigFile;

  var configXml = new ConfigFile(context.opts.projectRoot, null, './config.xml');

  // find the meta-data node in AndroidManifest.xml
  var androidPrjDir = path.join(context.opts.projectRoot, 'platforms/android/app/src/main');
  var androidManifest = new ConfigFile(androidPrjDir, 'android', 'AndroidManifest.xml');
  var applicationNode = androidManifest.data.find('application');

  // detect Parse notification icon
  var parsePushNotificationIcon = configXml.data.find('preference[@name="ParseNotificationIcon"]').get('value');
  if (!!parsePushNotificationIcon) {
    // add to AndroidManifest.xml
    var manifestPushNotificationIconNode = applicationNode.find('meta-data[@android:name="com.parse.push.notification_icon"]');

    if (!manifestPushNotificationIconNode) {
      manifestPushNotificationIconNode = new ET.Element('meta-data', { 'android:name': 'com.parse.push.notification_icon' });
      applicationNode.append(manifestPushNotificationIconNode);
    }
    manifestPushNotificationIconNode.set('android:resource', '@drawable/' + parsePushNotificationIcon);

    // COPY ICON
    // create target path
    var iconTargetPath = path.join(context.opts.projectRoot, 'platforms', 'android', 'app', 'src', 'main', 'res', 'drawable');
    try {
      fs.mkdirSync(iconTargetPath);
    } catch (err) {
      // Directory already exists
    }

    // copy icon to android folder
    fs
      .createReadStream(path.join(context.opts.projectRoot, 'resources', parsePushNotificationIcon + '.png'))
      .pipe(fs.createWriteStream(path.join(iconTargetPath, parsePushNotificationIcon + '.png')));
  }

  // Copy the firebase google-services.json file
  var googleServicesJson = path.join(context.opts.projectRoot, 'google-services.json');
  if(fs.existsSync(googleServicesJson)) {
    fs.createReadStream( googleServicesJson )
    .pipe(fs.createWriteStream(path.join(context.opts.projectRoot, 'platforms', 'android', 'app', 'google-services.json')));
  }else{
    console.error("Error: google-services.json missing! Get it from the Firebase console, and put it in the project root folder.");
  }


  androidManifest.save();

  return true;
}
