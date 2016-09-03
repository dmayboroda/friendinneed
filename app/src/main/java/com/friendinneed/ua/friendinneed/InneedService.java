package com.friendinneed.ua.friendinneed;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat;

/**
 * Created by Mayboroda on 8/30/16.
 */
public class InneedService extends Service {

    /** Use this constant for identify action which comes from service to MainActivity.  */
    public static final String MAIN_ACTION = InneedService.class.getSimpleName() + "_main";

    private static final String ACTION_START = InneedService.class.getSimpleName() + "_start";
    private static final String ACTION_STOP  = InneedService.class.getSimpleName() + "_stop";

    private static final int SERVICE_ID = 0110;

    public static void startInneedService(Context context) {
        Intent intent = new Intent(context, InneedService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    public static void stopInnedService(Context context) {
        Intent intent = new Intent(context, InneedService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_START)) {
            start();
        } else if (intent.getAction().equals(ACTION_STOP)) {
            stop();
        }
        return START_STICKY;
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void start() {
        startForeground(SERVICE_ID, createNotification());
        calculate();
    }

    private void stop() {
        stopForeground(true);
        stopSelf();
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.app_icon)
                .setLargeIcon(createBitmap())
                .setContentIntent(createIntent())
                .setOngoing(true)
                .build();
    }

    private PendingIntent createIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(MAIN_ACTION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                      | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pending = PendingIntent.getActivity(this, 0, intent, 0);
        return pending;
    }

    @WorkerThread
    private Bitmap createBitmap() {
        Bitmap bitmap = BitmapFactory.decodeResource
                (getResources(), R.drawable.app_icon);
        return Bitmap.createScaledBitmap(bitmap, 128, 128, false);
    }

    /** This method is for starting all calculations
     * that need to be in this service. */
    private void calculate() {

    }
}
