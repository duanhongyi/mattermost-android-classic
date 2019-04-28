package com.mattermost.xg;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.mattermost.mattermost.R;
import com.mattermost.mattermost.SplashScreenActivity;
import com.mattermost.service.MattermostService;
import com.tencent.android.tpush.XGPushBaseReceiver;
import com.tencent.android.tpush.XGPushRegisterResult;
import com.tencent.android.tpush.XGPushTextMessage;

import java.util.LinkedHashMap;

import org.json.JSONObject;

public class MessageReceiver extends BaseMessageReceiver {
    public static final int MESSAGE_NOTIFICATION_ID = 435345;
    public static final String GROUP_KEY_MESSAGES = "group_key_messages";
    private static LinkedHashMap<String,JSONObject> channelIdToNotification = new LinkedHashMap<String,JSONObject>();

    //注册的回调
    @Override
    public void onRegisterResult(Context context, int errorCode,
                                 XGPushRegisterResult message) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        if (context == null || message == null) {
            return;
        }
        String text = "";
        if (errorCode == XGPushBaseReceiver.SUCCESS) {
            text = message + "注册成功";
            // 在这里拿token
            String token = message.getToken();
            sharedPreferences.edit().putString("device_id", token).apply();
        } else {
            text = message + "注册失败错误码：" + errorCode;
        }
        Log.d(LogTag, text);
    }

    // 消息透传的回调
    @Override
    public void onTextMessage(Context context, XGPushTextMessage message) {
        try{
            String customContent = message.getCustomContent();
            if (customContent != null && customContent.length() != 0) {
                JSONObject data = new JSONObject(customContent);
                String type = data.getString("type");
                if ("clear".equals(type)) {
                    cancelNotification(context, data.getString("channel_id"));
                } else {
                    channelIdToNotification.put(data.getString("channel_id"), data);
                    createNotification(context, true);
                    String url = MattermostService.service.getBaseUrl();
                    String team = data.optString("team_id", null);
                    if (team == null || team.equals("")){
                        team = MattermostService.service.GetTeam();
                        if (team == null || team.equals("")){
                            team = MattermostService.service.GetLastPath().substring(
                                    MattermostService.service.getBaseUrl().length()+1);
                            team = team.substring(0, team.indexOf("/"));
                        }
                    }
                    url += "/"+ team +"/channels/" + data.getString("channel_id");
                    MattermostService.service.SetLastPath(url);

                }
            }
        } catch (org.json.JSONException e){
            Log.e(LogTag, e.getMessage());
        }
    }

    private void createNotification(Context context, boolean doAlert) throws org.json.JSONException{

        int defaults = 0;
        defaults = defaults | Notification.DEFAULT_LIGHTS
                | Notification.FLAG_AUTO_CANCEL;

        if (doAlert) {
            defaults = defaults | Notification.DEFAULT_VIBRATE
                    | Notification.DEFAULT_SOUND;
        }

        Intent notificationIntent = new Intent(context, SplashScreenActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIndent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, NotificationDismissReceiver.class), 0);

        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (channelIdToNotification.size() == 0) {
            mNotificationManager.cancel(MESSAGE_NOTIFICATION_ID);
            return;
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setGroup(GROUP_KEY_MESSAGES)
                .setDefaults(defaults)
                .setContentIntent(contentIndent)
                .setDeleteIntent(deleteIntent)
                .setAutoCancel(true);

        if (channelIdToNotification.size() == 1) {
            JSONObject data = channelIdToNotification.entrySet().iterator().next().getValue();
            String body = data.getString("message");
            mBuilder.setContentTitle("Mattermost")
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(body));
        } else {
            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

            String summaryTitle = String.format("Mattermost (%d)", channelIdToNotification.size());
            mBuilder.setContentTitle(summaryTitle);

            for (JSONObject data : channelIdToNotification.values()) {
                style.addLine(data.getString("message"));
            }

            style.setBigContentTitle(summaryTitle);
            mBuilder.setStyle(style);
        }

        mNotificationManager.cancel(MESSAGE_NOTIFICATION_ID);
        mNotificationManager.notify(MESSAGE_NOTIFICATION_ID, mBuilder.build());
    }
    private void cancelNotification(Context context, String channelId) throws org.json.JSONException{
        if (!channelIdToNotification.containsKey(channelId)) {
            return;
        }

        channelIdToNotification.remove(channelId);
        createNotification(context,false);
    }

    public static void clearNotifications() {
        channelIdToNotification.clear();
    }
}
