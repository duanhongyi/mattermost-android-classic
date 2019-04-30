package com.zhumengyuan.xg;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.tencent.android.tpush.XGPushConfig;

public class RegistrationIntentService extends IntentService {
    private static final String TAG = RegistrationIntentService.class.getName();

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        synchronized (TAG) {
            String token = XGPushConfig.getToken(this);
            Log.d(TAG, "XGPush registration token: " + token);
            sharedPreferences.edit().putString("device_id", token).apply();
        }
        Intent registrationComplete = new Intent(RegistrationConstants.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }
}
