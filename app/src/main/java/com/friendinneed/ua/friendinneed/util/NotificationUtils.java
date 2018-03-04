package com.friendinneed.ua.friendinneed.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.StringDef;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import com.friendinneed.ua.friendinneed.R;
import com.friendinneed.ua.friendinneed.SettingsActivity;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NotificationUtils {

    public static final String FIN_CHANNEL_ID = "FIN_CHANNEL_ID";

    @StringDef({
      FIN_CHANNEL_ID})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CompanioNotificationChannels {}

    /**
     * Ensures there is a channel with a given ID or creates one
     * @param context
     * @param channelId
     */
    public static void ensureNotificationChannel(Context context, @CompanioNotificationChannels String channelId) {
        String channelVisibleName = getChannelNameByChannelId(channelId);
        String description = getChannelDescriptionByChannelId(channelId);
        NotificationManager notificationManager = notificationManagerFrom(context);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            if (channel == null) {
                channel = new NotificationChannel(channelId, channelVisibleName, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(description);
                channel.enableLights(true);
                channel.setLightColor(Color.RED);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Name is visible from system settings page of the App and with every notification in the channel
     * @param channelId
     * @return
     */
    private static String getChannelNameByChannelId(@CompanioNotificationChannels String channelId) {
        switch (channelId){
            case FIN_CHANNEL_ID:
                return "Friend In Need";
            default:
                throw new RuntimeException("Non existing channel ID detected");
        }
    }

    /**
     * Description is visible from system settings page of the App
     * @param channelId
     * @return
     */
    private static String getChannelDescriptionByChannelId(@CompanioNotificationChannels String channelId) {
        switch (channelId){
            case FIN_CHANNEL_ID:
                return "Friend In Need Notifications";
            default:
                throw new RuntimeException("Non existing channel ID detected");
        }
    }

    public static PendingIntent createPendingIntent(Context context, Class<?> activityClass) {
        Intent resultIntent = new Intent(context, SettingsActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(activityClass);
        stackBuilder.addNextIntent(resultIntent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void showNotification(Context context, NotificationCompat.Builder builder, int id) {
        notificationManagerFrom(context).notify(id, builder.build());
    }

    public static void cancelNotification(Context context, int id) {
        notificationManagerFrom(context).cancel(id);
    }

    public static NotificationManager notificationManagerFrom(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static void cancelAll(Context context) {
        notificationManagerFrom(context).cancelAll();
    }

    /**
     * Prepares builder with default notification channel set to {@link NotificationUtils#FIN_CHANNEL_ID}
     * @param context
     * @param pendingIntent
     * @param contentTitle
     * @param contentText
     * @param progressEnabled
     * @param ongoing
     * @param groupId
     * @return
     */
    public static NotificationCompat.Builder prepareNotificationBuilder(Context context, PendingIntent pendingIntent, CharSequence contentTitle,
      CharSequence contentText, boolean progressEnabled, boolean ongoing, String groupId) {
        return prepareNotificationBuilder(context, pendingIntent, contentTitle, contentText, progressEnabled, ongoing, groupId, NotificationUtils.FIN_CHANNEL_ID);
    }

    /**
     * Prepares builder with given notification channel by ID
     * @param context
     * @param pendingIntent
     * @param contentTitle
     * @param contentText
     * @param progressEnabled
     * @param ongoing
     * @param groupId
     * @param channelId
     * @return
     */
    public static NotificationCompat.Builder prepareNotificationBuilder(Context context,
      PendingIntent pendingIntent,
      CharSequence contentTitle, CharSequence contentText, boolean progressEnabled,
      boolean ongoing, String groupId, @NotificationUtils.CompanioNotificationChannels String channelId) {
        NotificationUtils.ensureNotificationChannel(context, channelId);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
          .setContentTitle(contentTitle)
          .setContentText(contentText)
          .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
          .setSmallIcon(R.drawable.ic_notif)
          .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable
            .app_icon))
          .setContentIntent(pendingIntent)
          .setOngoing(ongoing)
          .setAutoCancel(true)
          .setTicker(contentTitle);
        if (groupId != null) {
            builder.setGroup(groupId);
            builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN);
        }
        if (progressEnabled) {
            builder.setProgress(0, 0, true);
        }
        return builder;
    }

    public static Notification prepareNotification(Context context,
      PendingIntent pendingIntent, CharSequence contentTitle, CharSequence contentText, boolean progressEnabled,
      boolean ongoing, String groupId) {
        return prepareNotificationBuilder(context, pendingIntent, contentTitle, contentText,
          progressEnabled, ongoing, groupId).build();
    }

    private NotificationUtils(){
        // private constructor for utils class
    }
}
