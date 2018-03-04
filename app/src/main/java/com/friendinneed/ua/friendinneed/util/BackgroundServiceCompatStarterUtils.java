package com.friendinneed.ua.friendinneed.util;

import android.content.Context;
import android.content.Intent;
import android.os.Build;


public class BackgroundServiceCompatStarterUtils {
    /**
     * please see https://developer.android.com/about/versions/oreo/android-8.0-changes
     * .html#back-all
     * section "Background execution limits"
     * <p>
     * The new Context.startForegroundService() method starts a foreground service.
     * The system allows apps to call Context.startForegroundService() even while the app is
     * in the background. However, the app must call that service's startForeground() method
     * within five seconds after the service is created.
     */
    public static void startServiceCompatFromBackground(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private BackgroundServiceCompatStarterUtils(){

    }
}
