import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';


///使用说明：
///   1. ios在plist.info中添加 "UmengKey"字段  Android 在AndroidManifest.xml 中添加
///   meta-data "umengKey"字段和"secret"字段
///   2. android 需要继承 FlutterApplication  并在 onCreate 函数中添加初始化代码
///   WorksUmengPlugin.initUmengSdk(this);
///   3. android如果需要支持厂商通道（只支持华为，小米，vivo，oppo），则在 AndroidManifest.xml
///   meta-data 中添加
///      .1 小米 XIAOMI_APP_ID 和 OPPO_APP_KEY
///      .2 oppo OPPO_APP_ID 和 OPPO_APP_KEY
///      .3 华为 com.huawei.hms.client.appid
///      .4 vivo com.vivo.push.api_key 和 com.vivo.push.app_id
///   4. 在dart类中调用WorksUmeng类对相关方法注册推送回调 type = 1 为点击推送  type = 2 为接收了推送


class WorksUmeng {
  static final  MethodChannel _channel = MethodChannel('works_umeng');

  static void initChannel()
  {
    _channel.setMethodCallHandler(methodHandler);
  }

  static Future<String> methodHandler (MethodCall call) async {
    if(call.method == 'deviceTokenSuccess')
    {
      _deviceToken = null;
      if(_deviceTokenCallback != null)
      {
        _deviceTokenCallback(call.arguments);
      }
      else
      {
        _deviceToken = call.arguments;
      }
    }
    else if(call.method == 'pushMessage')
    {
      if(_onData != null)
      {
        _onData(call.arguments);
      }
    }

    return null;
  }

  static ValueChanged _onData;

  static ValueChanged<String> _deviceTokenCallback;
  static String _deviceToken;

  static void receivePushListen(ValueChanged onData)
  {
    _onData = onData;
  }

  static void receiveRegisterForRemoteNotificationsToken(ValueChanged<String>
  onSuccess)
  {
    _deviceTokenCallback = onSuccess;
    if(_deviceToken != null)
    {
      onSuccess(_deviceToken);
    }
  }

}
