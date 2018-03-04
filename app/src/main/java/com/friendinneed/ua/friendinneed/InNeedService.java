package com.friendinneed.ua.friendinneed;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.friendinneed.ua.friendinneed.di.AppExecutors;
import com.friendinneed.ua.friendinneed.model.AccelerometerDataSample;
import com.friendinneed.ua.friendinneed.model.Contact;
import com.friendinneed.ua.friendinneed.model.DataSample;
import com.friendinneed.ua.friendinneed.model.DataSampleRequest;
import com.friendinneed.ua.friendinneed.model.GyroscopeDataSample;
import com.friendinneed.ua.friendinneed.util.NotificationUtils;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.friendinneed.ua.friendinneed.BuildConfig.DEBUG;
import static com.friendinneed.ua.friendinneed.util.BackgroundServiceCompatStarterUtils.startServiceCompatFromBackground;
import static java.lang.Math.sqrt;

public class InNeedService extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
    InNeedContract.View {

  public static final String SOS_MESSAGE_PREFIX = "SOS message - bingo!, http://maps.google.com/maps?z=16&q=loc:";
  public static final String SOS_MESSAGE_NO_LOCATION = "I need Your help, call me please.";
  public static final int MILLIS_IN_SECOND = 1000;
  public static final String FENCE_RECEIVER_ACTION =
      BuildConfig.APPLICATION_ID + ".FENCE_RECEIVER_ACTION";
  public static final String START_FENCE_KEY = "start_fence_key";
  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  private static final String TAG = InNeedService.class.getSimpleName();
  private static final String ACTION_START = TAG + "_start";
  private static final String ACTION_STOP = TAG + "_stop";
  private static final String ACTION_START_DETECTION = TAG + "_start_detection";
  private static final String ACTION_STOP_DETECTION = TAG + "_stop_detection";
  private static final int SERVICE_ID = 0110;
  private static final double EPSILON = 0.0;
  private static final int QUEUE_TIMEOUT_MILLIS = 2000;
  private static final int DEFAULT_WAIT_TIME = 15;
  private static final String BASE_URL = "http://friendinneedserver6099.cloudapp.net";
  private static final String CHECK_FALL_ENDPOINT = "/check_fall";
  private static final int PORT = 5000;
  private static AtomicBoolean isCheckingData = new AtomicBoolean(false);
  private static AtomicBoolean isDetecting = new AtomicBoolean(false);
  private final DataQueue queue = new DataQueue(QUEUE_TIMEOUT_MILLIS);
  AlertDialog dataCollectionDialog;
  AlertDialog countDownDialog;
  Vibrator mVibrator;
  @Inject AppExecutors appExecutors;
  @Inject
  InNeedApi inNeedApi;
  @Inject
  Repository repository;
  @Inject
  InNeedPresenter presenter;
  private GoogleApiClient mGoogleApiClient;
  private boolean labelingEnabled = true;
  private OkHttpClient client;
  private JoltCalculator joltCalculator;
  private AsyncTask<Integer, Void, Void> requestSendAsyncTask = getSendDataTask();
  private StillStateReceiver mFenceReceiver;
  private SensorManager mSensorManager;
  private Sensor mAccelerometerSensor;
  private Sensor mGyroSensor;
  private Sensor mSignificantMotionSensor;
  private CountDownTimer userCountDownTimer;
  private CountDownTimer inactivityFirstTimer;
  private DialogInterface.OnClickListener onSendDataCollectionDialogClickListener =
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          sendDataRequest(1);
          dialog.dismiss();
          resumeInnedService(getApplicationContext());
        }
      };
  private DialogInterface.OnClickListener onDiscardDataCollectionDialogClickListener =
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          sendDataRequest(0);
          dialog.dismiss();
          resumeInnedService(getApplicationContext());
        }
      };
  private DialogInterface.OnClickListener onDiscardFallClickListener = new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int id) {
      if (userCountDownTimer != null) {
        userCountDownTimer.cancel();
      }
      dialog.dismiss();
      resumeInnedService(getApplicationContext());
    }
  };
  private TriggerEventListener mTriggerInactivityTimerCanceller = new TriggerEventListener() {
    @Override
    public void onTrigger(TriggerEvent event) {
      resumeInnedService(getApplicationContext());
      if (inactivityFirstTimer != null) {
        inactivityFirstTimer.cancel();
        isCheckingData.set(false);
      }
    }
  };
  // TODO this one shoud be used to sop detecting while still
  private TriggerEventListener mTriggerEventListener = new TriggerEventListener() {
    @Override
    public void onTrigger(TriggerEvent event) {
      resumeInnedService(getApplicationContext());
      //if (inactivityFirstTimer != null) {
      //    inactivityFirstTimer.cancel();
      //    isCheckingData.set(false);
      //}
    }
  };
  private TriggerEventListener mCountdownOffTriggerEventListener = new TriggerEventListener() {
    @Override
    public void onTrigger(TriggerEvent event) {
      resumeInnedService(getApplicationContext());
      if (userCountDownTimer != null) {
        userCountDownTimer.cancel();
        countDownDialog.dismiss();
        isCheckingData.set(false);
      }
    }
  };
  private LocationManager locationManager;
  private AsyncTask<Void, Void, Void> requestCheckAsyncTask = getSendCheckDataTask();
  private DialogInterface.OnClickListener onSendFallClickListener = new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialog, int id) {
      if (userCountDownTimer != null) {
        userCountDownTimer.cancel();
      }
      callForHelp();
      dialog.dismiss();
      resumeInnedService(getApplicationContext());
    }
  };

  public InNeedService() {
  }

  public static void startInneedService(Context context) {
    Intent intent = new Intent(context, InNeedService.class);
    intent.setAction(ACTION_START);
    startServiceCompatFromBackground(context, intent);
  }

  /**
   * To be called from UI
   * @param context
   */
  public static void stopInnedService(Context context) {
    Intent intent = new Intent(context, InNeedService.class);
    intent.setAction(ACTION_STOP);
    context.startService(intent);
  }

  public static void suspendInnedService(Context context) {
    Intent intent = new Intent(context, InNeedService.class);
    intent.setAction(ACTION_STOP_DETECTION);
    startServiceCompatFromBackground(context, intent);
  }

  public static void resumeInnedService(Context context) {
    Intent intent = new Intent(context, InNeedService.class);
    intent.setAction(ACTION_START_DETECTION);
    startServiceCompatFromBackground(context, intent);
  }

  @Override public void onCreate() {
    super.onCreate();
    FriendApp.getComponent().inject(this);
    NotificationUtils.ensureNotificationChannel(this, NotificationUtils.FIN_CHANNEL_ID);
    presenter.onTakeView(this);
    joltCalculator = new JoltCalculator(queue);
    locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (null != intent) {
      startForeground(SERVICE_ID, createNotification());
      if (intent.getAction().equals(ACTION_START)) {
        start();
      } else if (intent.getAction().equals(ACTION_STOP)) {
        stop();
      } else if (intent.getAction().equals(ACTION_START_DETECTION)) {
        startDetection();
      } else if (intent.getAction().equals(ACTION_STOP_DETECTION)) {
        stopDetection();
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
    labelingEnabled = getSharedPreferences(
        SettingsActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE).
        getBoolean(SettingsActivity.LABELING_ENABLED, false);
    mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addApi(Awareness.API)
        .addConnectionCallbacks(this)
        .build();
    OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
    clientBuilder.connectTimeout(20000, TimeUnit.MILLISECONDS);
    clientBuilder.retryOnConnectionFailure(false);
    client = clientBuilder.build();
    dataCollectionDialog = getDataCollectionDialog();
    countDownDialog = getCountdownDialog();
    mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    mSignificantMotionSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
    startDetection();
    calculate();
  }

  private void startDetection() {
    if (isDetecting.get()) {
      return;
    }
    mGoogleApiClient.connect();
    mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);
    mSensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
    mSensorManager.cancelTriggerSensor(mTriggerEventListener, mSignificantMotionSensor);
    isDetecting.set(true);
  }

  private void stopDetection() {
    if (!isDetecting.get()) {
      return;
    }
    if (mAccelerometerSensor != null && mGyroSensor != null) {
      mSensorManager.unregisterListener(this, mAccelerometerSensor);
      mSensorManager.unregisterListener(this, mGyroSensor);
    }
    if (mFenceReceiver != null) {
      unregisterReceiver(mFenceReceiver);
    }

    mSensorManager.requestTriggerSensor(mTriggerEventListener, mSignificantMotionSensor);
    isDetecting.set(false);
  }

  private void stop() {
    stopDetection();
    stopForeground(true);
    Awareness.FenceApi.updateFences(
        mGoogleApiClient,
        new FenceUpdateRequest.Builder()
            .removeFence(START_FENCE_KEY)
            .build())
        .setResultCallback(new ResultCallback<Status>() {
          @Override
          public void onResult(@NonNull Status status) {
            mGoogleApiClient.disconnect();
            if (status.isSuccess()) {
              Log.i(TAG, "Fence was successfully unregistered.");
            } else {
              Log.e(TAG, "Fence could not be unregistered: " + status);
            }
          }
        });
    stopSelf();
  }

  private Notification createNotification() {
    return new NotificationCompat.Builder(this, NotificationUtils.FIN_CHANNEL_ID)
        .setContentTitle(getString(R.string.is_protecting_you))
        .setSmallIcon(R.drawable.ic_notif)
        .setLargeIcon(createBitmap())
        .setContentIntent(createIntent())
        .setOngoing(true)
        .build();
  }

  private PendingIntent createIntent() {
    Intent intent = new Intent(this, SettingsActivity.class);
    intent.setAction(SettingsActivity.SERVICE_ACTION);
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
    presenter.checkFall(new DataSampleRequest(queue.getDataSamples(), 0));
  }

  void sendDataRequest(final int label) {
    presenter.sendTestFallData(new DataSampleRequest(queue.getDataSamples(), label));
  }

  @Nullable
  private AlertDialog getDataCollectionDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.alertDialog);
    builder.setTitle(R.string.dialog_labeling_title);
    builder.setPositiveButton(R.string.yes_txt, onSendDataCollectionDialogClickListener);
    builder.setNegativeButton(R.string.no_txt, onDiscardDataCollectionDialogClickListener);
    builder.setCancelable(false);
    AlertDialog dialog = builder.create();
    Window dialogWindow = dialog.getWindow();
    if (dialogWindow != null) {
      dialogWindow.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
      return dialog;
    }
    return null;
  }

  @Nullable
  private AlertDialog getCountdownDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.alertDialog);
    builder.setTitle(R.string.dialog_title);
    builder.setPositiveButton(R.string.send, onSendFallClickListener);
    builder.setNegativeButton(R.string.discard, onDiscardFallClickListener);
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
    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      onAccelerometerSensorChanged(event);
    } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
      onGyroSensorChanged(event);
    }
  }

  private void onAccelerometerSensorChanged(SensorEvent event) {
    float accX;
    float accY;
    float accZ;
    accX = event.values[0];
    accY = event.values[1];
    accZ = event.values[2];
    final DataSample sampleAccelerometer = new AccelerometerDataSample(accX, accY, accZ);
    queue.addSample(sampleAccelerometer);
    if (DEBUG) {
      //Log.v(TAG, "queue size: " + String.valueOf(queue.size()));
    }
    if (requestSendAsyncTask.getStatus() != AsyncTask.Status.RUNNING ||
        requestCheckAsyncTask.getStatus() != AsyncTask.Status.RUNNING) {
      if (!isCheckingData.get() && joltCalculator.checkForJolt(accX, accY, accZ)) {
        //                    Log.v(TAG, "accX=" + accX + ", accY=" + accY + ", accZ=" + accZ);
        if (labelingEnabled) {
          if (!dataCollectionDialog.isShowing()) {
            dataCollectionDialog.show();
            mVibrator.vibrate(800);
          }
        } else {
          fallMaybeHappen();
        }
      }
    }
  }

  private void onGyroSensorChanged(SensorEvent event) {
    float gyroX;
    float gyroY;
    float gyroZ;
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

  private void fallMaybeHappen() {
    isCheckingData.set(true);
    inactivityFirstTimer = new CountDownTimer(getSharedPreferences(
        SettingsActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE).
        getInt(SettingsActivity.TIME_TO_WAIT, DEFAULT_WAIT_TIME) * MILLIS_IN_SECOND,
        MILLIS_IN_SECOND) {
      @Override
      public void onTick(long millisUntilFinished) {
        Log.d(TAG, "inactivityFirstTimer is ticking: " +
            (millisUntilFinished / MILLIS_IN_SECOND));
      }

      @Override
      public void onFinish() {
        mSensorManager.cancelTriggerSensor(mTriggerInactivityTimerCanceller, mSignificantMotionSensor);
        Log.d(TAG, "checking if real fall on server... ");
        sendCheckRequest();
      }
    }.start();

    mSensorManager.requestTriggerSensor(mTriggerInactivityTimerCanceller, mSignificantMotionSensor);
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }

  private void callForHelp() {

    Log.i(TAG, "callForHelp");
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      // we never reach here, but studio is asking us to leave this code here
      return;
    }
    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    String message;
    if (location != null) {
      Log.d(TAG, "callForHelp location is not null, composing message...");
      message = SOS_MESSAGE_PREFIX + location.getLatitude() + "," + location.getLongitude();
    } else {
      Log.d(TAG, "callForHelp location is  null");
      if (canToggleGPS()) {
        Log.d(TAG, "callForHelp location is null, can toggle gps, trying...");
        turnGPSOn();
        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Log.d(TAG, "callForHelp, composing message after try to toggle and got location..." +
            String.valueOf(location != null));
        message = (location != null) ? SOS_MESSAGE_PREFIX + location.getLatitude() + "," + location.getLongitude() :
            SOS_MESSAGE_NO_LOCATION;
      } else {
        Log.d(TAG, "callForHelp location is null, composing no location message...");
        message =
            SOS_MESSAGE_NO_LOCATION;
      }
    }
    SmsManager sms = SmsManager.getDefault();

    ArrayList<Contact> contactsArray = SettingsActivity.getContactsListSharePref(this);
    try {
      for (Contact contact : contactsArray) {
        Log.d(TAG, "callForHelp sending sms to " + contact.getContactNumber());
        sms.sendTextMessage(contact.getContactNumber(), null, message, null, null);
      }
      Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
    } catch (Exception e) {
      Toast.makeText(getApplicationContext(), "SMS failed, please try again.", Toast.LENGTH_LONG).show();
      e.printStackTrace();
    }
  }

  private AsyncTask<Integer, Void, Void> getSendDataTask() {
    return new AsyncTask<Integer, Void, Void>() {
      String response;

      @Override
      protected Void doInBackground(Integer... params) {
        try {
          Log.d(TAG, "sending labeled data with label " + params[0]);
          response = postDataQueueSamples(BASE_URL + ":" + PORT, queue.getDataSamples(), params[0]);
        } catch (IOException e) {
          e.printStackTrace();
        }
        return null;
      }

      @Override
      protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (response != null) {
          Log.d(TAG, "success sent labeled data to server");
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
          Log.d(TAG, "sending check request to server...");
          response = postDataQueueSamples(BASE_URL + ":" + PORT + CHECK_FALL_ENDPOINT, queue.getDataSamples(),
              0); //label does not matter here
        } catch (IOException e) {
          Log.d(TAG, "sending check request to server... error while sending");
          e.printStackTrace();
        }
        return null;
      }

      @Override
      protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.d(TAG, "getting result from server check...");
        if (response != null) {
          Log.v(TAG, response);
          if (Boolean.TRUE == Boolean.parseBoolean(response)) {
            countDownDialog.show();

            mVibrator.vibrate(800);

            userCountDownTimer = new CountDownTimer(getSharedPreferences(
                SettingsActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE).
                getInt(SettingsActivity.TIME_TO_WAIT, DEFAULT_WAIT_TIME) * MILLIS_IN_SECOND,
                MILLIS_IN_SECOND) {
              @Override
              public void onTick(long millisUntilFinished) {
                countDownDialog.setTitle(String.format(getString(R.string.dialog_title_formatted),
                    (millisUntilFinished / MILLIS_IN_SECOND)));
                Log.d(TAG, "userCountDownTimer is ticking: " +
                    (millisUntilFinished / MILLIS_IN_SECOND));
              }

              @Override
              public void onFinish() {
                Log.d(TAG, "Calling for help");
                callForHelp();
                mSensorManager.cancelTriggerSensor(mCountdownOffTriggerEventListener,
                    mSignificantMotionSensor);
                countDownDialog.dismiss();
                isCheckingData.set(false);
                cancel();
              }
            }.start();

            mSensorManager.requestTriggerSensor(mCountdownOffTriggerEventListener,
                mSignificantMotionSensor);
          } else {
            isCheckingData.set(false);
          }
        } else {
          isCheckingData.set(false);
          Log.v(TAG, "no connection most probably");
        }
      }
    };
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    mFenceReceiver = new StillStateReceiver();
    registerReceiver(mFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));
    AwarenessFence stillStartFence = DetectedActivityFence.during(DetectedActivityFence.STILL);

    Intent startIntent = new Intent(FENCE_RECEIVER_ACTION);
    String FENCE_KEY = "fence_key";
    startIntent.putExtra(FENCE_KEY, START_FENCE_KEY);
    PendingIntent mStartPendingIntent =
        PendingIntent.getBroadcast(InNeedService.this, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    Awareness.FenceApi.updateFences(
        mGoogleApiClient,
        new FenceUpdateRequest.Builder()
            .addFence(START_FENCE_KEY, stillStartFence, mStartPendingIntent)
            .build())
        .setResultCallback(new ResultCallback<Status>() {
          @Override
          public void onResult(@NonNull Status status) {
            if (status.isSuccess()) {
              Log.i(TAG, "Fence was successfully registered.");
            } else {
              Log.e(TAG, "Fence could not be registered: " + status);
            }
          }
        });
  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  private boolean canToggleGPS() {
    PackageManager packageManager = getPackageManager();
    PackageInfo pacInfo = null;

    try {
      pacInfo = packageManager.getPackageInfo("com.android.settings", PackageManager.GET_RECEIVERS);
    } catch (PackageManager.NameNotFoundException e) {
      return false; //package not found
    }

    if (pacInfo != null) {
      for (ActivityInfo actInfo : pacInfo.receivers) {
        //test if recevier is exported. if so, we can toggle GPS.
        if (actInfo.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && actInfo.exported) {
          return true;
        }
      }
    }

    return false; //default
  }

  private void turnGPSOn() {
    String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

    if (!provider.contains("gps")) { //if gps is disabled
      final Intent poke = new Intent();
      poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
      poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
      poke.setData(Uri.parse("3"));
      sendBroadcast(poke);
    }
  }

  private void turnGPSOff() {
    String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

    if (provider.contains("gps")) { //if gps is enabled
      final Intent poke = new Intent();
      poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
      poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
      poke.setData(Uri.parse("3"));
      sendBroadcast(poke);
    }
  }

  @Override public void onCheckFallSuccess(String result) {
    if (Boolean.TRUE == Boolean.parseBoolean(result)) {
      countDownDialog.show();
      mVibrator.vibrate(800);
      userCountDownTimer = new CountDownTimer(getSharedPreferences(
          SettingsActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE).
          getInt(SettingsActivity.TIME_TO_WAIT, DEFAULT_WAIT_TIME) * MILLIS_IN_SECOND,
          MILLIS_IN_SECOND) {
        @Override
        public void onTick(long millisUntilFinished) {
          countDownDialog.setTitle(String.format(getString(R.string.dialog_title_formatted),
              (millisUntilFinished / MILLIS_IN_SECOND)));
          Log.d(TAG, "userCountDownTimer is ticking: " +
              (millisUntilFinished / MILLIS_IN_SECOND));
        }

        @Override
        public void onFinish() {
          Log.d(TAG, "Calling for help");
          callForHelp();
          mSensorManager.cancelTriggerSensor(mCountdownOffTriggerEventListener,
              mSignificantMotionSensor);
          countDownDialog.dismiss();
          isCheckingData.set(false);
          cancel();
        }
      }.start();
      mSensorManager.requestTriggerSensor(mCountdownOffTriggerEventListener,
          mSignificantMotionSensor);
    } else {
      isCheckingData.set(false);
    }
  }

  @Override public void onCheckFallFailure(Throwable t) {
    isCheckingData.set(false);
    Log.v(TAG, "no connection most probably");
  }

  @Override public void onSendFallSuccess(String result) {
    Log.d(TAG, "success sent labeled data to server");
    Log.v(TAG, result);
  }

  @Override public void onSendFallFailure(Throwable t) {
    Log.d(TAG, "error sending labeled data to server: " + t.getMessage());
  }
}
