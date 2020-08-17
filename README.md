# works_umeng

友盟插件，支持ios和android.

##usage

# 引入

```yaml
  dependencies:
    flutter:
      sdk: flutter
    asset_picker:
    #本地路径
      path: /**/flutter_works_umeng
#或者git地址
#	  git:
#       url: git://github.com/cdworks/flutter_works_umeng.git
```

#ios配置

ios在plist.info中添加 "UmengKey"字段

#android配置

1. Android 在AndroidManifest.xml 中添加 meta-data "umengKey"字段和"secret"字段
2. android 需要继承 FlutterApplication  并在 onCreate 函数中添加初始化代码  WorksUmengPlugin.initUmengSdk(this);
3. android如果需要支持厂商通道（只支持华为，小米，vivo，oppo），则在 AndroidManifest.xml meta-data 中添加
   	- 小米 XIAOMI_APP_ID 和 OPPO_APP_KEY
    - oppo OPPO_APP_ID 和 OPPO_APP_KEY
    - 华为 com.huawei.hms.client.appid
    - vivo com.vivo.push.api_key 和 com.vivo.push.app_id

#示例代码

```dart
void main() 
{
  //初始化
  WorksUmeng.initChannel();
  
  //接收推送消息回调
  WorksUmeng.receivePushListen((pushMessage)
  {
     int type = message['type']; // type==1 表示点击推送事件。type==3 表示接收到推送消息
     if(type == null)
     	return;
     Map messageData = message['data']; //data字段表示推送原始数据，根据实际情况解析
  });

  //device token 回调

  WorksUmeng.receiveRegisterForRemoteNotificationsToken((token)
  {
    print('device token:$token');
  });
}
```
