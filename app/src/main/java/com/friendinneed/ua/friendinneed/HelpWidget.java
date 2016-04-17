package com.friendinneed.ua.friendinneed;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Created by Viktor Pasichnyk on 4/16/16.
 */
public class HelpWidget extends AppWidgetProvider {

    public static String IMAGE_CLICK = "image_click";
    private static boolean state = false;
    private RemoteViews remoteViews;
    String LOG = "123";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        updateSwitch(context, appWidgetManager);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.i(LOG, "onReceive");
        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(context);

        if (IMAGE_CLICK.equals(intent.getAction())) {
            if(!state){
                Log.i(LOG, "ifno");
                remoteViews.setImageViewResource(R.id.widget_image1, R.drawable.widget_on);
            }else {
                Log.i(LOG, "ifyes");
                remoteViews.setImageViewResource(R.id.widget_image1, R.drawable.widget_off);
            }
            state = !state;
        }

        updateSwitch(context, appWidgetManager);
        appWidgetManager.updateAppWidget(new ComponentName(context,
                HelpWidget.class), remoteViews);
    }

    private void updateSwitch(Context context, AppWidgetManager appWidgetManager){

        Log.i(LOG, "updateSwitch");
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
        Intent imageClickIntent = new Intent(context, HelpWidget.class);
        imageClickIntent.setAction(IMAGE_CLICK);
        PendingIntent imageViewPandingIntent = PendingIntent.getBroadcast(context, 0, imageClickIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.widget_image1, imageViewPandingIntent);
        appWidgetManager.updateAppWidget(new ComponentName(context,
                HelpWidget.class), remoteViews);
    }
}
