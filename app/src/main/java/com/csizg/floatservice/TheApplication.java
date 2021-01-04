package com.csizg.floatservice;

import android.app.Application;
import android.content.Context;

public class TheApplication extends Application {
    private static TheApplication sInstance;

    //获取全局Context
    public static Context getAppContext() {
        return sInstance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }
}
