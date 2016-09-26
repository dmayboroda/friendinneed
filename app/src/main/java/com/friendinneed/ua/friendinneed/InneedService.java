package com.friendinneed.ua.friendinneed;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.friendinneed.ua.friendinneed.model.AccelerometerDataSample;
import com.friendinneed.ua.friendinneed.model.DataSample;
import com.friendinneed.ua.friendinneed.model.DataSampleRequest;
import com.friendinneed.ua.friendinneed.model.GyroscopeDataSample;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.friendinneed.ua.friendinneed.BuildConfig.DEBUG;
import static java.lang.Math.sqrt;

/**
 * Created by Mayboroda on 8/30/16.
 */
public class InneedService extends Service implements SensorEventListener {

    private static final boolean isCheckDialogVersion = true;
    private static final String TAG = InneedService.class.getSimpleName();
    private static final String ACTION_START = TAG + "_start";
    private static final String ACTION_STOP = TAG + "_stop";

    private static final int SERVICE_ID = 0110;

    private static final double G_POINT = 1.8 * 9.8;
    private static final double EPSILON = 0.0;
    private static final int QUEUE_TIMEOUT_MILLIS = 2000;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String BASE_URL = "http://friendinneedserver6099.cloudapp.net";
    private static final String CHECK_FALL_ENDPOINT = "/check_fall";
    private static final int PORT = 5000;
    private final OkHttpClient client = new OkHttpClient();
    private final DataQueue queue = new DataQueue(QUEUE_TIMEOUT_MILLIS);
    private AsyncTask<Integer, Void, Void> requestSendAsyncTask = getSendDataTask();
    private AsyncTask<Void, Void, Void> requestCheckAsyncTask = getSendCheckDataTask();
    private DialogInterface.OnClickListener onSendClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            sendDataRequest(1);
            dialog.dismiss();
        }
    };
    private DialogInterface.OnClickListener onDiscardClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            sendDataRequest(0);
            dialog.dismiss();
        }
    };

    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private Sensor mGyroSensor;
    AlertDialog dialog;
    Vibrator mVibrator;

    public InneedService() {
    }

    private AsyncTask<Integer, Void, Void> getSendDataTask() {
        return new AsyncTask<Integer, Void, Void>() {
            String response;

            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    response = postDataQueueSamples(BASE_URL + ":" + PORT, queue.dump(), params[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (response != null) {
                    Log.v(TAG, response);
                } else {
                    Log.v(TAG, "no connection most probably");
                }
            }
        };
    }

    private AsyncTask<Void, Void, Void> getSendCheckDataTask() {
        return new AsyncTask<Void, Void, Void>() {
            String response;

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    response = postDataQueueSamples(BASE_URL + ":" + PORT + CHECK_FALL_ENDPOINT, queue.dump(), 0); //label does not matter here
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (response != null) {
                    Log.v(TAG, response);
                    //TODO: check server response and maybe call for help
                    //callForHelp();
                } else {
                    Log.v(TAG, "no connection most probably");
                }
            }
        };
    }

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
        if (null != intent) {
            if (intent.getAction().equals(ACTION_START)) {
                start();
            } else if (intent.getAction().equals(ACTION_STOP)) {
                stop();
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void start() {
        dialog = getDialog();
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        startForeground(SERVICE_ID, createNotification());
        calculate();
    }

    private void stop() {
        if (mAccelerometerSensor != null && mGyroSensor != null) {
            mSensorManager.unregisterListener(this, mAccelerometerSensor);
            mSensorManager.unregisterListener(this, mGyroSensor);
        }
        stopForeground(true);
        stopSelf();
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_notif)
                .setLargeIcon(createBitmap())
                .setContentIntent(createIntent())
                .setOngoing(true)
                .build();
    }

    private PendingIntent createIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(MainActivity.SERVICE_ACTION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    @WorkerThread
    private Bitmap createBitmap() {
        Bitmap bitmap = BitmapFactory.decodeResource
                (getResources(), R.drawable.ic_notif);
        return Bitmap.createScaledBitmap(bitmap, 128, 128, false);
    }

    /**
     * This method is for starting all calculations
     * that need to be in this service.
     */
    private void calculate() {
    }

    String postDataQueueSamples(String url, DataSample[] dataSamples, Integer label) throws IOException {
        DataSampleRequest dataSampleRequest = new DataSampleRequest(dataSamples, label);
        RequestBody body = RequestBody.create(JSON, new Gson().toJson(dataSampleRequest));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    void sendCheckRequest() {
        requestCheckAsyncTask = getSendCheckDataTask();
        requestCheckAsyncTask.execute();
    }

    void sendDataRequest(int label) {
        requestSendAsyncTask = getSendDataTask();
        requestSendAsyncTask.execute(label);
    }

    @Nullable
    private AlertDialog getDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.alertDialog));
        builder.setTitle(R.string.dialog_title);
        builder.setPositiveButton(R.string.send, onSendClickListener);
        builder.setNegativeButton(R.string.discard, onDiscardClickListener);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        Window dialogWindow = dialog.getWindow();
        if (dialogWindow != null) {
            dialogWindow.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            return dialog;
        }
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float accX, accY, accZ, gyroX, gyroY, gyroZ;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accX = event.values[0];
            accY = event.values[1];
            accZ = event.values[2];
            final DataSample sampleAccelerometer = new AccelerometerDataSample(accX, accY, accZ);
            queue.addSample(sampleAccelerometer);
            if (DEBUG) {
                Log.d(TAG, "queue size: " + String.valueOf(queue.size()));
            }
            if (requestSendAsyncTask.getStatus() != AsyncTask.Status.RUNNING ||
                    requestCheckAsyncTask.getStatus() != AsyncTask.Status.RUNNING) {
                if (checkForJolt(accX, accY, accZ)) {
                    //Log.v(TAG, "accX=" + accX + ", accY=" + accY + ", accZ=" + accZ);
                    if (isCheckDialogVersion) {
                        if (!dialog.isShowing()) {
                            dialog.show();
                            mVibrator.vibrate(800);
                        }
                    } else {
                        sendCheckRequest();
                    }
                }
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroX = event.values[0];
            gyroY = event.values[1];
            gyroZ = event.values[2];

            // Calculate the angular speed of the sample
            double omegaMagnitude = sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ);

            // Normalize the rotation vector if it's big enough to get the axis
            // (that is, EPSILON should represent your maximum allowable margin of error)
            if (omegaMagnitude > EPSILON) {
                gyroX /= omegaMagnitude;
                gyroY /= omegaMagnitude;
                gyroZ /= omegaMagnitude;
            }
            final DataSample sampleGyroscope = new GyroscopeDataSample(gyroX, gyroY, gyroZ);
            queue.addSample(sampleGyroscope);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void callForHelp() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String phone = SharedPrefsUtils.getStringPreference(this, "contacts2");
            smsManager.sendTextMessage(TextUtils.isEmpty(phone) ? "+380976673962" : phone, null, "SOS message - bingo!, http://maps.google.com/maps?z=16&q=loc:50.4174038,30.474646", null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "SMS failed, please try again.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private boolean checkForJolt(float accX, float accY, float accZ) {
        float sum = (accX * accX) + (accY * accY) + (accZ * accZ);
        double checkVar = Math.sqrt(Double.parseDouble(Float.toString(sum)));
        return (checkVar > G_POINT);
    }
}
