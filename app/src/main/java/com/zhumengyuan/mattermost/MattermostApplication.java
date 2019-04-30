/**
 * Copyright (c) 2016 Mattermost, Inc. All Rights Reserved.
 * See License.txt for license information.
 */
package com.zhumengyuan.mattermost;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGPushConfig;
import com.tencent.android.tpush.XGPushManager;
import com.zhumengyuan.service.MattermostService;

import java.io.PrintWriter;
import java.io.StringWriter;

//import android.webkit.CookieSyncManager;

public class MattermostApplication extends Application {

    public static MattermostApplication current;
    public static Handler handler;

    private static final String TAG = MattermostApplication.class.getName();

    public MattermostApplication() {
        super();
        current = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //      CookieSyncManager.createInstance(this);
        MattermostService.service = new MattermostService(this);
        //xinge push
        registerPush();

        handler = new Handler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                ex.printStackTrace();
            }
        });

    }

    public static String toString(Throwable ex) {
        String msg = ex.toString();
        StringWriter sw = null;
        PrintWriter pw = null;
        try {
            sw = new StringWriter();
            pw = new PrintWriter(sw);

            ex.printStackTrace(pw);

            pw.flush();

            msg = sw.toString();
        } catch (Exception e) {
        } finally {
            try {
                if (pw != null)
                    pw.close();
                if (sw != null)
                    sw.close();
            } catch (Exception doNothing) {
            }
        }

        return msg;
    }

    public static void logError(Exception ex) {
        Log.e("Error", toString(ex));
    }

    private void registerPush(){
        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        XGPushConfig.enableDebug(this, true);
        XGPushConfig.enableOtherPush(getApplicationContext(), true);
        XGPushConfig.setHuaweiDebug(true);
        XGPushConfig.setMiPushAppId(getApplicationContext(), "2882303761517996365");
        XGPushConfig.setMiPushAppKey(getApplicationContext(), "5141799624365");
        XGPushConfig.setMzPushAppId(this, "120303");
        XGPushConfig.setMzPushAppKey(this, "632b2a967cd3408c98f148bf60dcb01c");
        XGPushManager.registerPush(this, new XGIOperateCallback() {
            public void onSuccess(Object token, int code) {
                Log.i(TAG, "XG register push success with token : " + token);
                sharedPreferences.edit().putString("device_id", token.toString()).apply();
            }
            public void onFail(Object token, int errCode, String msg) {
                Log.e(TAG, "XG register push failed with token : " + token + ", errCode : " + errCode + " , msg : " + msg);
            }
        });
    }
}