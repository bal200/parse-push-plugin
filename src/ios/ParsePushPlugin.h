#import <Cordova/CDV.h>
#import "AppDelegate.h"
#import <UserNotifications/UserNotifications.h>

@interface ParsePushPlugin: CDVPlugin <UNUserNotificationCenterDelegate>

@property bool hasRegistered;
@property (copy) NSString* callbackId;
@property (retain) NSMutableArray* pnQueue;

//
// methods exposed to JS as API
- (void)register: (CDVInvokedUrlCommand *)command;
- (void)getInstallationId: (CDVInvokedUrlCommand*)command;
- (void)getInstallationObjectId: (CDVInvokedUrlCommand*)command;

- (void)getSubscriptions: (CDVInvokedUrlCommand *)command;
- (void)subscribe: (CDVInvokedUrlCommand *)command;
- (void)unsubscribe: (CDVInvokedUrlCommand *)command;
- (void)resetBadge: (CDVInvokedUrlCommand *)command;
- (void)getLocation: (CDVInvokedUrlCommand *)command;
- (void)setLocation: (CDVInvokedUrlCommand *)command;

//
// methods exposed to JS but not intended for users (not part of API)
- (void)registerCallback: (CDVInvokedUrlCommand*)command;

//
// methods internal to plugin
- (void)pluginInitialize;
- (void)registerForPN;
- (void)jsCallback: (NSDictionary*)userInfo withAction: (NSString*)pnAction;
- (NSString *) getConfigForKey:(NSString *) key;
+ (void)saveDeviceTokenToInstallation: (NSData*)deviceToken;
@end
