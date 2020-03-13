package com.works.works_umeng_example;

import com.works.works_umeng.WorksUmengPlugin;

public class WorksUmengAppliation extends  io.flutter.app.FlutterApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        WorksUmengPlugin.initUmengSdk(this);
    }
}
