package com.works.works_umeng;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.message.*;
import com.umeng.message.entity.UMessage;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.JSONUtil;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import org.android.agoo.huawei.HuaWeiRegister;
import org.android.agoo.oppo.OppoRegister;
import org.android.agoo.vivo.VivoRegister;
import org.android.agoo.xiaomi.MiPushRegistar;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * WorksUmengPlugin
 */
public class WorksUmengPlugin implements FlutterPlugin, MethodCallHandler {

  static final String TAG = "umengsdk";

  static MethodChannel _methodChannel;


  public static void saveUmengMessage(Context context, String value) {
    SharedPreferences userSettings = context.getSharedPreferences("works_umeng_push", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = userSettings.edit();
    editor.putString("umengMessage", value);
    editor.apply();
  }

  public static void saveDeviceToken(Context context, String token) {
    SharedPreferences userSettings = context.getSharedPreferences("works_umeng_push", Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = userSettings.edit();
    editor.putString("deviceToken", token);
    editor.apply();
  }

  public static String getUmengMessage(Context context) {
    SharedPreferences userSettings = context.getSharedPreferences("works_umeng_push", Context.MODE_PRIVATE);
    return userSettings.getString("umengMessage", null);
  }

  public static String getDeviceToken(Context context) {
    SharedPreferences userSettings = context.getSharedPreferences("works_umeng_push", Context.MODE_PRIVATE);
    return userSettings.getString("deviceToken", null);
  }

  public static boolean initUmengSdk(final Application context) {
    UMConfigure.setLogEnabled(false);
    try {
      ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
      if (applicationInfo.metaData == null) {
        Log.e(TAG, "error: umeng 'key' and 'Secret' not found!,please set umeng key by adding <meta-data umengkey> to AndroidManifest.xml!");
        return false;
      }
      Object msg = applicationInfo.metaData.get("umengKey");
      Object secret = applicationInfo.metaData.get("secret");
      if (msg == null || secret == null) {
        Log.e(TAG, "error: umeng 'key' and 'Secret' not found!,please set umeng key by adding <meta-data umengkey> to AndroidManifest.xml!");
        return false;
      }

      final String msgString = String.valueOf(msg);
      String secretString = String.valueOf(secret);
      UMConfigure.init(context, msgString, "umeng", UMConfigure.DEVICE_TYPE_PHONE,
              secretString);

      PushAgent pushAgent = PushAgent.getInstance(context);

      pushAgent.setNotificationPlaySound(MsgConstant.NOTIFICATION_PLAY_SERVER); //服务端控制声音
      pushAgent.setNotificationPlayLights(MsgConstant.NOTIFICATION_PLAY_SDK_ENABLE);//客户端允许呼吸灯点亮
      pushAgent.setNoDisturbMode(0,0,0,0); //关闭免打扰模式
      pushAgent.setDisplayNotificationNumber(5);
      pushAgent.setNotificaitonOnForeground(true);

      pushAgent.onAppStart();

      pushAgent.register(new IUmengRegisterCallback() {
        @Override
        public void onSuccess(final String s) {
          if (_methodChannel != null) {
            new Handler(context.getMainLooper()).post(new Runnable() {
              @Override
              public void run() {
                _methodChannel.invokeMethod("deviceTokenSuccess", s);
              }
            });
          } else {
            saveDeviceToken(context, s);
          }
        }

        @Override
        public void onFailure(String s, String s1) {
          Log.e(TAG, "umeng 注册token失败:" + "s:" + s + ",s1:" + s1);
        }
      });

      UmengNotificationClickHandler notificationClickHandler = new UmengNotificationClickHandler() {
        public void launchApp(Context context, final UMessage uMessage) {

          if (_methodChannel != null) {

            new Handler(context.getMainLooper()).post(new Runnable() {
              @Override
              public void run() {
                Map<String, Object> message = new HashMap<>();
                message.put("type", 1);
                message.put("data", formatMsg(uMessage));
                _methodChannel.invokeMethod("pushMessage", message);
              }
            });

          } else {
            JSONObject jsonObject = (JSONObject) JSONUtil.wrap(formatMsg(uMessage));
            saveUmengMessage(context, jsonObject.toString());
          }

          super.launchApp(context, uMessage);
        }

        public void openUrl(Context context, UMessage uMessage) {
          super.openUrl(context, uMessage);
        }

        public void openActivity(Context context, UMessage uMessage) {
          super.openActivity(context, uMessage);
        }

        @Override
        public void dealWithCustomAction(Context context, UMessage msg) {
          super.dealWithCustomAction(context, msg);
        }
      };

//      pushAgent.setMessageHandler();

      pushAgent.setNotificationClickHandler(notificationClickHandler);

      pushAgent.setMessageHandler(new UmengMessageHandler()
      {
        @Override
        public Notification getNotification(Context context, final UMessage uMessage) {
          if (_methodChannel != null) {
            new Handler(context.getMainLooper()).post(new Runnable() {
              @Override
              public void run() {
                Map<String, Object> message = new HashMap<>();
                message.put("type", 3);
                message.put("data", formatMsg(uMessage));
                _methodChannel.invokeMethod("pushMessage", message);
              }
            });
          }
          return super.getNotification(context, uMessage);
        }

        @Override
        public void dealWithCustomMessage(Context context, final UMessage uMessage) {
          if (_methodChannel != null) {
            new Handler(context.getMainLooper()).post(new Runnable() {
              @Override
              public void run() {
                Map<String, Object> message = new HashMap<>();
                message.put("type", 3);
                message.put("data", formatMsg(uMessage));
                _methodChannel.invokeMethod("pushMessage", message);
              }
            });
          }
          super.dealWithCustomMessage(context, uMessage);
        }
      });

      String appId = applicationInfo.metaData.getString("com.huawei.hms.client.appid");

      /*注册厂商渠道*/
      /*华为*/
      if(appId != null)
      {
        HuaWeiRegister.register(context);
      }

      /*VIVO*/
      Object appIdObject = applicationInfo.metaData.get("com.vivo.push.api_key");
      if(appIdObject != null)
      {
        VivoRegister.register(context);
      }

      appIdObject = applicationInfo.metaData.get("XIAOMI_APP_ID");
      Object appKeyObject = applicationInfo.metaData.get("XIAOMI_APP_KEY");

      /*小米*/
      if(appIdObject != null && appKeyObject != null)
      {
        MiPushRegistar.register(context,appIdObject.toString(), appKeyObject.toString());
      }

      appIdObject = applicationInfo.metaData.get("OPPO_APP_ID");
      appKeyObject = applicationInfo.metaData.get("OPPO_APP_KEY");

      /*OPPO*/
      if(appIdObject != null && appKeyObject != null)
      {
        OppoRegister.register(context,appIdObject.toString(), appKeyObject.toString());
      }
      return true;
    } catch (Exception e) {
      Log.e(TAG, "get umeng key excption:" + e.getLocalizedMessage());
      return false;
    }
  }


  static Map<String, Object> formatMsg(UMessage msg) {
    Map<String, Object> message = new HashMap<>();
    message.put("msg_id", msg.msg_id == null ? "" : msg.msg_id);
    message.put("display_type", msg.display_type == null ? "" : msg.display_type);
    message.put("random_min", msg.random_min);
    message.put("title", msg.title);
    message.put("text", msg.text);
    message.put("play_vibrate", msg.play_vibrate);
    message.put("play_lights", msg.play_lights);
    message.put("play_sound", msg.play_sound);
    message.put("screen_on", msg.screen_on);
    message.put("url", msg.url);
    message.put("img", msg.img);
    message.put("sound", msg.sound);
    message.put("icon", msg.icon);
    message.put("after_open", msg.after_open);
    message.put("largeIcon", msg.largeIcon);
    message.put("activity", msg.activity);
    message.put("custom", msg.custom);
    message.put("recall", msg.recall);
    message.put("bar_image", msg.bar_image);
    message.put("expand_image", msg.expand_image);
    message.put("builder_id", msg.builder_id);
    message.put("isAction", msg.isAction);
    message.put("pulled_service", msg.pulled_service);
    message.put("pulled_package", msg.pulled_package);
    message.put("pulledWho", msg.pulledWho);

    if (msg.extra != null) {
      message.put("extra", msg.extra);
    }
    return message;
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

    if (WorksUmengPlugin._methodChannel == null) {
      WorksUmengPlugin._methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "works_umeng");
    }

    WorksUmengPlugin._methodChannel.setMethodCallHandler(this);

    String token = getDeviceToken(flutterPluginBinding.getApplicationContext());

    if (token != null) {
      _methodChannel.invokeMethod("deviceTokenSuccess", token);
      saveDeviceToken(flutterPluginBinding.getApplicationContext(), null);
    }

    String messageString = getUmengMessage(flutterPluginBinding.getApplicationContext());
    if (messageString != null) {
      try {
        JSONObject jsonObject = new JSONObject(messageString);

        Map<String, Object> message = new HashMap<>();
        message.put("type", 1);
        message.put("data", JSONUtil.unwrap(jsonObject));
        _methodChannel.invokeMethod("pushMessage", message);

      } catch (JSONException ex) {
      }

      saveUmengMessage(flutterPluginBinding.getApplicationContext(), null);
    }

  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  public static void registerWith(Registrar registrar) {
    if (WorksUmengPlugin._methodChannel == null) {
      WorksUmengPlugin._methodChannel = new MethodChannel(registrar.messenger(), "works_umeng");
      WorksUmengPlugin plugin = new WorksUmengPlugin();
      WorksUmengPlugin._methodChannel.setMethodCallHandler(plugin);


      String token = getDeviceToken(registrar.context());

      if (token != null) {
        _methodChannel.invokeMethod("deviceTokenSuccess", token);
        saveDeviceToken(registrar.context(), null);
      }

      String messageString = getUmengMessage(registrar.context());
      if (messageString != null) {
        try {
          JSONObject jsonObject = new JSONObject(messageString);

          Map<String, Object> message = new HashMap<>();
          message.put("type", 1);
          message.put("data", JSONUtil.unwrap(jsonObject));
          _methodChannel.invokeMethod("pushMessage", message);

        } catch (JSONException ex) {
        }

        saveUmengMessage(registrar.context(), null);
      }

    }
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {


  }

}
