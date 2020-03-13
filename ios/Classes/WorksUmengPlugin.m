#import "WorksUmengPlugin.h"
#import <UMCommon/UMCommon.h>
#import <UMPush/UMessage.h>

@interface WorksUmengPlugin()
@property (nonatomic) FlutterMethodChannel* methodChannel;
@end

@implementation WorksUmengPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"works_umeng"
            binaryMessenger:[registrar messenger]];
  WorksUmengPlugin* instance = [[WorksUmengPlugin alloc] init];
    instance.methodChannel = channel;
  [registrar addMethodCallDelegate:instance channel:channel];

//    FlutterEventChannel *eventChannel = [FlutterEventChannel eventChannelWithName:@"works_push_event" binaryMessenger:[registrar messenger]];
//    [eventChannel setStreamHandler:instance];
    [registrar addApplicationDelegate:instance];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getPlatformVersion" isEqualToString:call.method]) {
    result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
  } else {
    result(FlutterMethodNotImplemented);
  }
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions{


    [UMConfigure setLogEnabled:NO];

    NSString* umengkey = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"UmengKey"];
    if(!umengkey)
    {
        NSLog(@"error: umeng key not found!,please set umeng key by info.plist!");
        return YES;
    }



    [UMConfigure initWithAppkey:umengkey channel:@"App Store"];

    // Push功能配置
    UMessageRegisterEntity * entity = [[UMessageRegisterEntity alloc] init];
    entity.types = UMessageAuthorizationOptionBadge|UMessageAuthorizationOptionAlert|UMessageAuthorizationOptionSound;

    //如果要在iOS10显示交互式的通知，必须注意实现以下代码
    if (@available(iOS 10.0, *)) {
        UNNotificationAction *action1_ios10 = [UNNotificationAction actionWithIdentifier:@"action1_identifier" title:@"打开应用" options:UNNotificationActionOptionForeground];
        UNNotificationAction *action2_ios10 = [UNNotificationAction actionWithIdentifier:@"action2_identifier" title:@"忽略" options:UNNotificationActionOptionForeground];

        //UNNotificationCategoryOptionNone
        //UNNotificationCategoryOptionCustomDismissAction  清除通知被触发会走通知的代理方法
        //UNNotificationCategoryOptionAllowInCarPlay       适用于行车模式
        UNNotificationCategory *category1_ios10 = [UNNotificationCategory categoryWithIdentifier:@"category1" actions:@[action1_ios10,action2_ios10]   intentIdentifiers:@[] options:UNNotificationCategoryOptionCustomDismissAction];
        NSSet *categories = [NSSet setWithObjects:category1_ios10, nil];
        entity.categories=categories;
        [UNUserNotificationCenter currentNotificationCenter].delegate=self;
    }

    //如果你期望使用交互式(只有iOS 8.0及以上有)的通知，请参考下面注释部分的初始化代码
    if (([[[UIDevice currentDevice] systemVersion]intValue]>=8)&&([[[UIDevice currentDevice] systemVersion]intValue]<10)) {
        UIMutableUserNotificationAction *action1 = [[UIMutableUserNotificationAction alloc] init];
        action1.identifier = @"action1_identifier";
        action1.title=@"打开应用";
        action1.activationMode = UIUserNotificationActivationModeForeground;//当点击的时候启动程序

        UIMutableUserNotificationAction *action2 = [[UIMutableUserNotificationAction alloc] init];  //第二按钮
        action2.identifier = @"action2_identifier";
        action2.title=@"忽略";
        action2.activationMode = UIUserNotificationActivationModeBackground;//当点击的时候不启动程序，在后台处理
        action2.authenticationRequired = YES;//需要解锁才能处理，如果action.activationMode = UIUserNotificationActivationModeForeground;则这个属性被忽略；
        action2.destructive = YES;
        UIMutableUserNotificationCategory *actionCategory1 = [[UIMutableUserNotificationCategory alloc] init];
        actionCategory1.identifier = @"category1";//这组动作的唯一标示
        [actionCategory1 setActions:@[action1,action2] forContext:(UIUserNotificationActionContextDefault)];
        NSSet *categories = [NSSet setWithObjects:actionCategory1, nil];
        entity.categories=categories;
    }


    [UMessage registerForRemoteNotificationsWithLaunchOptions:launchOptions Entity:entity completionHandler:^(BOOL granted, NSError * _Nullable error) {
        if (granted) {
        }else{
        }
    }];

    if(launchOptions){
        NSDictionary *userInfo = [launchOptions objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey];
        if(userInfo)
        {

            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(3 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{

                [self->_methodChannel invokeMethod:@"pushMessage" arguments:@{@"type":@(1),@"data":userInfo}];

            });
        }
    }

    return YES;
}

- (void)application:(UIApplication*)application
didRegisterForRemoteNotificationsWithDeviceToken:(NSData*)deviceToken
{
    if (![deviceToken isKindOfClass:[NSData class]])
        return;
    const unsigned *tokenBytes = (const unsigned *)[deviceToken bytes];
    NSString *hexToken = [NSString stringWithFormat:@"%08x%08x%08x%08x%08x%08x%08x%08x",
                          ntohl(tokenBytes[0]), ntohl(tokenBytes[1]), ntohl(tokenBytes[2]),
                          ntohl(tokenBytes[3]), ntohl(tokenBytes[4]), ntohl(tokenBytes[5]),
                          ntohl(tokenBytes[6]), ntohl(tokenBytes[7])];
    
    [_methodChannel invokeMethod:@"deviceTokenSuccess" arguments:hexToken];
    
    //    if(_eventSink)
    //    {
    //        _eventSink(@{@"type":@(0),@"data":hexToken});
    //    }
}



//iOS10以下使用这两个方法接收通知，
//  1. iOS 8-iOS10 系统的普通推送，APP处于前台运行或者点击远程推送消息进入APP都会来到这个方法
//  2. 只要是静默推送消息，会调用此方法。
-(BOOL)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler
{
    [UMessage setAutoAlert:NO];
    if([[[UIDevice currentDevice] systemVersion]intValue] < 10){
        [UMessage didReceiveRemoteNotification:userInfo];

        [self->_methodChannel invokeMethod:@"pushMessage" arguments:@{@"type":@(1),@"data":userInfo}];


        //    self.userInfo = userInfo;
        //    //定制自定的的弹出框
        //    if([UIApplication sharedApplication].applicationState == UIApplicationStateActive)
        //    {
        //        UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:@"标题"
        //                                                            message:@"Test On ApplicationStateActive"
        //                                                           delegate:self
        //                                                  cancelButtonTitle:@"确定"
        //                                                  otherButtonTitles:nil];
        //
        //        [alertView show];
        //
        //    }
        completionHandler(UIBackgroundFetchResultNewData);
    }
    return YES;
}

// 方法B：iOS10以上的方法，适用于iOS10以上设备
// 推送消息到达时用户APP正处于前台运行中会调用这个方法

-(void)userNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(nonnull UNNotification *)notification withCompletionHandler:(nonnull void (^)(UNNotificationPresentationOptions))completionHandler
API_AVAILABLE(ios(10.0)){
    NSDictionary * userInfo = notification.request.content.userInfo;

    if([notification.request.trigger isKindOfClass:[UNPushNotificationTrigger class]]) {
        //应用处于前台时的远程推送接受
        //关闭友盟自带的弹出框
        [UMessage setAutoAlert:NO];
        //必须加这句代码
        [UMessage didReceiveRemoteNotification:userInfo];
        [self->_methodChannel invokeMethod:@"pushMessage" arguments:@{@"type":@(3),@"data":userInfo}];

    }else{
        //应用处于前台时的本地推送接受
    }
    //当应用处于前台时提示设置，需要哪个可以设置哪一个
    completionHandler(UNNotificationPresentationOptionSound|UNNotificationPresentationOptionBadge|UNNotificationPresentationOptionAlert);
}




//iOS10新增：处理后台点击通知的代理方法
// 用户通过点击推送通知进入APP时会调用这个方法
-(void)userNotificationCenter:(UNUserNotificationCenter *)center didReceiveNotificationResponse:(UNNotificationResponse *)response withCompletionHandler:(void (^)(void))completionHandler API_AVAILABLE(ios(10.0)){



    NSDictionary * userInfo = response.notification.request.content.userInfo;

    if([response.notification.request.trigger isKindOfClass:[UNPushNotificationTrigger class]]) {
        //应用处于后台时的远程推送接受
        //必须加这句代码
        [UMessage didReceiveRemoteNotification:userInfo];
        [self->_methodChannel invokeMethod:@"pushMessage" arguments:@{@"type":@(1),@"data":userInfo}];
        //         UIViewController* rootController = self.window.rootViewController;
        //        if ([rootController isKindOfClass:[MainTabbarViewController class]]) {
        //            MainTabbarViewController* controller = (MainTabbarViewController*)rootController;
        //            ExceptionGateInOutViewController* eController = [ExceptionGateInOutViewController new];
        //
        //            eController.view.backgroundColor = [UIColor whiteColor];
        //            [(UINavigationController*)controller.selectedViewController pushViewController:eController animated:YES];
        ////            [(UINavigationController*)controller.selectedViewController pushViewController:[UIViewController new] animated:YES];
        ////             viewControllers[controller.selectedIndex] pushViewController:[UIViewController new] animated:YES];
        //            _noticeDic = nil;
        //        }
        //        else
        //        {
        //            _noticeDic = userInfo;
        //        }
    }

    completionHandler();

}


@end
