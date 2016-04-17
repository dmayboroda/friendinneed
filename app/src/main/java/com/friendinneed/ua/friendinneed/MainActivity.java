package com.friendinneed.ua.friendinneed;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    private static final double EPSILON = 0.0;
    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private Sensor mGyroSensor;
    private float accX;
    private float accY;
    private float accZ;
    private float gyroX;
    private float gyroY;
    private float gyroZ;
    List<List<Double>> sample;
    List<List<Double>> labels;
    private final double G_POINT = 1.3 * 9.8;

    boolean writeData = false;

    Button manageContactsButton;
    Timer sendToTensorTimer = new Timer();
    TimerTask sendToTensorTimerTask = new TimerTask() {
        @Override
        public void run() {
            sendRequest();
        }
    };
    private int count;
    private TimerTask tTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_FASTEST);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startSavingRawData();

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        manageContactsButton = (Button) findViewById(R.id.button_manage_contacts);
        manageContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), ContactsActivity.class);
                startActivity(intent);
            }
        });

    }

    private void startSavingRawData() {
        //start writing of data
        sample = new ArrayList<List<Double>>();
        labels = new ArrayList<List<Double>>();
        writeData = true;
        AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
        adb.setTitle("Did You fall?").setPositiveButton("YES!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                writeData = false;
                save(true);
                callForHelp();
//                sendRequest();
            }
        }).setNegativeButton("NO!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                writeData = false;
                save(false);
            }
        }).show();
        sendToTensorTimer.schedule(sendToTensorTimerTask, 10000);
        final Timer t =new Timer();
        count = 10;
        tTask = new TimerTask() {
            @Override
            public void run() {

                Toast.makeText(MainActivity.this, "" + count, Toast.LENGTH_SHORT);
                count--;
                t.schedule(tTask, 1000);
            }
        };
        t.schedule(tTask, 1000);
    }


    private String readFromFile(String path) {

        String ret = "";

        try {
            InputStream inputStream = new FileInputStream(new File(path));

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        try {
//            Log.i("123test_raw", readFromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/file_data"));
//            Log.i("123test_labels", readFromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/file_labels"));
////            Toast.makeText(getBaseContext(), s,Toast.LENGTH_SHORT).show();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void save(boolean isFall) {

        for (int i = 0; i < sample.size(); i++) {
            List<Double> label = new ArrayList<>();
            if (/*i < sample.size() - 1 ||*/ !isFall) {
                label.add(0.);
                label.add(1.);
            } else {
                label.add(1.);
                label.add(0.);
            }
            labels.add(label);
        }
        /*
        //write to file
        try {
             // true will be same as Context.MODE_APPEND
//            FileOutputStream fileout = openFileOutput(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/file_data", MODE_APPEND);
            FileOutputStream fileout = new FileOutputStream (new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/file_data"), true);
            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
            StringBuffer sb = new StringBuffer();
            for (List<Double> data : sample) {
                for (Double d : data) {
                    sb.append(String.valueOf(d)).append(",");
                }
                sb.append("\r\n");
            }

            outputWriter.write(sb.toString());
            outputWriter.close();

            FileOutputStream fileoutLabels = new FileOutputStream (new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/file_labels"), true);
            OutputStreamWriter outputWriterLabels = new OutputStreamWriter(fileoutLabels);
            StringBuffer sbLabels = new StringBuffer();
            for (List<Double> data : labels) {
                for (Double d : data) {
                    sbLabels.append(String.valueOf(d)).append(",");
                }
                sbLabels.append("\r\n");
            }

            outputWriterLabels.write(sbLabels.toString());
            outputWriterLabels.close();

            //display file saved message
            Toast.makeText(getBaseContext(), "File saved successfully!",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camara) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accX = event.values[0];
            accY = event.values[1];
            accZ = event.values[2];
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

        }
        if (checkForJolt()) {
            startSavingRawData();
        }
        //Log.i("raw_data", "accX=" + accX + ", accY=" + accY + ", accZ=" + accZ + ", gyroX=" + gyroX + ", gyroY="+ gyroY +", gyroZ=" +gyroZ );
        if (writeData) {
            List<Double> data = new ArrayList<>();
            data.add((double) accX);
            data.add((double) accY);
            data.add((double) accZ);
            data.add((double) gyroX);
            data.add((double) gyroY);
            data.add((double) gyroZ);
            sample.add(data);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    public OkHttpClient client = new OkHttpClient();
    void sendRequest(){
        new AsyncTask<Void, Void, Void>() {
            String response;


            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Log.i("response = ", response );
                if ("action_fall".equals(response)) {
                    callForHelp();
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    response = post("http://192.168.43.184:8087", new Gson().toJson(sample));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

    }

    private void callForHelp() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String phone = SharedPrefsUtils.getStringPreference(MainActivity.this, "phone");
            smsManager.sendTextMessage(TextUtils.isEmpty(phone) ? "+380976673962" : phone, null, "SOS message - bingo!, geo:50.4174038,30.474646", null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "SMS failed, please try again.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    private boolean checkForJolt() {
        float sum = (accX * accX) + (accY * accY) + (accZ * accZ);
        double checkVar = Math.sqrt(Double.parseDouble(Float.toString(sum)));
        return (checkVar > G_POINT);
    }
}
