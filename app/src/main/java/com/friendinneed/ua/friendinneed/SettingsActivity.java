package com.friendinneed.ua.friendinneed;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.util.PlotStatistics;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.friendinneed.ua.friendinneed.model.Contact;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    public static final String SERVICE_ACTION = SettingsActivity.class.getSimpleName() + "_service";
    private Boolean trackingStatus = false;
    private SharedPreferences prefs;
    private SwitchCompat trackingSwitch;
    private TextView trackingSwitchTextView;
    private NumberPicker numberPicker;

    private String phoneNumber;
    private ContactListAdapter contactListAdapter;
    private static final int defaultTimeToWait = 15;
    private int timeToWait;
    private Activity context;

    public static final String SHARED_PREFS_NAME = "frieninneed";
    public static final String CONTACTS = "contacts";
    public static final String TRACKING_STATUS = "tracking_status";
    public static final String TIME_TO_WAIT = "time_to_wait";
    public static final int PICK_CONTACT = 10;
    private static final int PICK_CONTACT_REQUEST = 20;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS_FOR_ADD = 150;
    public static final int PERMISSIONS_REQUEST_SMS_LOCATION = 200;
    public static final int SYSTEM_ALERTS_REQUEST_CODE = 400;
    public static final String[] LOCATION_SMS_PERMISSIONS =
          { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS };

    private ArrayList<Contact> contactArrayList;

    private double maxGValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        context = this;
        prefs = getBaseContext().getSharedPreferences(SHARED_PREFS_NAME,
              Context.MODE_PRIVATE);

        contactArrayList = new ArrayList<>();

        Button addContactBtn = (Button) findViewById(R.id.choose_contact_btn);
        addContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readContact();
            }
        });

        final Button createAndAddContact = (Button) findViewById(R.id.create_contact_btn);
        createAndAddContact.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                createAndAddContact();
            }
        });

        trackingSwitchTextView = (TextView) findViewById(R.id.tracking_switch_text);
        trackingSwitch = (SwitchCompat) findViewById(R.id.tracking_switch);
        numberPicker = (NumberPicker) findViewById(R.id.numb_picker_settings);
        numberPicker.setWrapSelectorWheel(false);
        timeToWait = prefs.getInt(TIME_TO_WAIT, defaultTimeToWait);

        numberPicker.setTag(timeToWait);
        numberPicker.setValue(timeToWait);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                timeToWait = newVal;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(TIME_TO_WAIT, newVal);
                editor.commit();
            }
        });
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(60);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        trackingStatus = prefs.getBoolean(TRACKING_STATUS, false);
        trackingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (hasLocatinSmsPermissiongs(context, LOCATION_SMS_PERMISSIONS) && checkDrawOverlayPermission()) {
                        startServiceSaveUI();
                    } else {
                        addDrawOverlayPermission();
                        ActivityCompat.requestPermissions(context, new String[] {
                                    Manifest.permission.SEND_SMS,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                              },
                              PERMISSIONS_REQUEST_SMS_LOCATION);
                    }
                } else {
                    InNeedService.stopInnedService(SettingsActivity.this);
                    trackingSwitchTextView.setText(getResources().getString(R.string.turn_on_tracking));
                    trackingStatus = false;
                    savingTrackStatusToSharePref(trackingStatus);
                }
            }
        });
        trackingSwitch.setChecked(trackingStatus);

        ListView contactsAddedListView = (ListView) findViewById(R.id.contacts_added_list);
        contactArrayList = getContactsListSharePref(this);
        if (contactArrayList == null) {
            contactArrayList = new ArrayList<>();
        }
        contactListAdapter = new ContactListAdapter(this, R.layout.contact_list_item, contactArrayList);
        contactsAddedListView.setAdapter(contactListAdapter);
        contactsAddedListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setMessage(getString(R.string.delete_contact_txt));
                builder.setPositiveButton(getString(R.string.yes_txt), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        contactArrayList.remove(position);
                        contactListAdapter.notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton(getString(R.string.no_txt), null);
                builder.show();

                return true;
            }
        });

        if (BuildConfig.DEBUG) {
            initDebugTools();
        }
    }

    private void initDebugTools() {
        final LinearLayout contentWrapper = (LinearLayout) findViewById(R.id.content_wrap);
        final TextView gValueTextView = new TextView(this);

        gValueTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                gValueTextView.setText(String.valueOf(0));
                maxGValue = 0;
                return false;
            }
        });
        XYPlot accelerometerHistoryPlot = new XYPlot(this, "accelerometer", Plot.RenderMode.USE_BACKGROUND_THREAD);
        accelerometerHistoryPlot.getGraphWidget().setShowRangeLabels(true);

        final SimpleXYSeries xHistorySeries = new SimpleXYSeries("X");
        final SimpleXYSeries yHistorySeries = new SimpleXYSeries("Y");
        final SimpleXYSeries zHistorySeries = new SimpleXYSeries("Z");
        xHistorySeries.useImplicitXVals();
        yHistorySeries.useImplicitXVals();
        zHistorySeries.useImplicitXVals();

        accelerometerHistoryPlot.setGridPadding(100, 50, 0, 0);
        accelerometerHistoryPlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 5.0d);

        accelerometerHistoryPlot.setDomainBoundaries(0, 500,
              BoundaryMode.FIXED);             // number of points to plot in history
        accelerometerHistoryPlot.addSeries(xHistorySeries,
              new LineAndPointFormatter(
                    Color.rgb(100, 100, 200), null, null, null));
        accelerometerHistoryPlot.addSeries(yHistorySeries,
              new LineAndPointFormatter(
                    Color.rgb(100, 200, 100), null, null, null));
        accelerometerHistoryPlot.addSeries(zHistorySeries,
              new LineAndPointFormatter(
                    Color.rgb(200, 100, 100), null, null, null));

        final PlotStatistics histStats = new PlotStatistics(1000, false);

        accelerometerHistoryPlot.addListener(histStats);

        SensorManager sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        final SimpleXYSeries xSeries = new SimpleXYSeries("X");
        final SimpleXYSeries ySeries = new SimpleXYSeries("Y");
        final SimpleXYSeries zSeries = new SimpleXYSeries("Z");

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float accX = sensorEvent.values[0];
                float accY = sensorEvent.values[1];
                float accZ = sensorEvent.values[2];
                double gValue = JoltCalculator.calculateGValue(accX, accY, accZ) / JoltCalculator.GRAVITY;

                if (gValue > maxGValue) {
                    gValueTextView.setText(String.valueOf(gValue));
                    maxGValue = gValue;
                }

                xSeries.setModel(Collections.singletonList(
                      accX),
                      SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

                ySeries.setModel(Collections.singletonList(
                      accY),
                      SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

                zSeries.setModel(Collections.singletonList(
                      accZ),
                      SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

                // get rid the oldest sample in history:
                if (zHistorySeries.size() > 500) {            // number of points to plot in history
                    zHistorySeries.removeFirst();
                    yHistorySeries.removeFirst();
                    xHistorySeries.removeFirst();
                }

                // add the latest history sample:
                xHistorySeries.addLast(null, accX);
                yHistorySeries.addLast(null, accY);
                zHistorySeries.addLast(null, accZ);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        }, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);

        Redrawer redrawer = new Redrawer(
              Arrays.asList(new Plot[] { accelerometerHistoryPlot }),
              100, false);

        contentWrapper.addView(gValueTextView);
        contentWrapper.addView(accelerometerHistoryPlot);
        redrawer.start();
    }

    private void startServiceSaveUI() {
        InNeedService.startInneedService(context);
        trackingSwitchTextView.setText(getResources().getString(R.string.tracking_is_running));
        trackingStatus = true;
        savingTrackStatusToSharePref(trackingStatus);
    }

    private boolean hasLocatinSmsPermissiongs(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        trackingStatus = prefs.getBoolean(TRACKING_STATUS, false);
        trackingSwitch.setChecked(trackingStatus);

        timeToWait = prefs.getInt(TIME_TO_WAIT, defaultTimeToWait);
        numberPicker.setTag(timeToWait);
        numberPicker.setValue(timeToWait);
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveContactsListSharePref(this, contactArrayList);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PICK_CONTACT:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    getContactDataFromUri(uri);
                }
                break;
            case PICK_CONTACT_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    getContactDataFromUri(uri);
                }
                break;
            case SYSTEM_ALERTS_REQUEST_CODE:
                if (hasLocatinSmsPermissiongs(context, LOCATION_SMS_PERMISSIONS) && checkDrawOverlayPermission()) {
                    startServiceSaveUI();
                    trackingSwitch.setChecked(trackingStatus);
                } else {
                    Toast.makeText(SettingsActivity.this, R.string.location_sms_windows_features_perm_accept,
                          Toast.LENGTH_LONG)
                          .show();
                }
                break;
            default:
                //nothn
        }
    }

    public void readContact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
              && checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
              && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.READ_CONTACTS }, PERMISSIONS_REQUEST_READ_CONTACTS);
        }
    }

    private void createAndAddContact() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
              && checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(Intent.ACTION_INSERT);
            i.setType(ContactsContract.Contacts.CONTENT_TYPE);
            if (Integer.valueOf(Build.VERSION.SDK) > 14) {
                i.putExtra("finishActivityOnSaveCompleted", true); // Fix for 4.0.3 +
            }
            startActivityForResult(i, PICK_CONTACT_REQUEST);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
              && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.READ_CONTACTS },
                  PERMISSIONS_REQUEST_READ_CONTACTS_FOR_ADD);
        }
    }

    public void saveContactsListSharePref(Context context, List<Contact> contactListToShare) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME,
              Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String jsonContacts = gson.toJson(contactListToShare);
        editor.putString(CONTACTS, jsonContacts);
        editor.commit();
    }

    public static ArrayList<Contact> getContactsListSharePref(Context context) {
        List<Contact> sharedContactList = new ArrayList<>();

        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME,
              Context.MODE_PRIVATE);

        if (sharedPreferences.contains(CONTACTS)) {
            String jsonContacts = sharedPreferences.getString(CONTACTS, null);
            Gson gson = new Gson();
            Contact[] contactsArray = gson.fromJson(jsonContacts,
                  Contact[].class);

            sharedContactList = Arrays.asList(contactsArray);
            sharedContactList = new ArrayList<>(sharedContactList);
        } else {
            return (ArrayList<Contact>) sharedContactList;
        }
        return (ArrayList<Contact>) sharedContactList;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_CONTACTS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readContact();
                } else {
                    Toast.makeText(SettingsActivity.this, R.string.contacts_perm_accept, Toast.LENGTH_SHORT)
                          .show();
                }
                break;
            case PERMISSIONS_REQUEST_READ_CONTACTS_FOR_ADD:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createAndAddContact();
                } else {
                    Toast.makeText(SettingsActivity.this, R.string.contacts_perm_accept, Toast.LENGTH_SHORT)
                          .show();
                }
                break;
            case PERMISSIONS_REQUEST_SMS_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                      && grantResults[1] == PackageManager.PERMISSION_GRANTED
                      && grantResults[2] == PackageManager.PERMISSION_GRANTED
                      && checkDrawOverlayPermission()) {
                    startServiceSaveUI();
                    trackingSwitch.setChecked(trackingStatus);
                } else {
                    Toast.makeText(SettingsActivity.this, R.string.location_sms_windows_features_perm_accept,
                          Toast.LENGTH_LONG)
                          .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void savingTrackStatusToSharePref(boolean trackingStatus) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(TRACKING_STATUS, trackingStatus);
        editor.commit();
    }

    public void addDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                      Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, SYSTEM_ALERTS_REQUEST_CODE);
            }
        }
    }

    public boolean checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return false;
        } else {
            return true;
        }
    }

    public void getContactDataFromUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        ContentResolver cr = getContentResolver();
        String contactID = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

        // get phone number
        if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

            Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                  ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                  new String[] { contactID }, null);
            while (pCur.moveToNext()) {
                phoneNumber = pCur.getString(
                      pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
            pCur.close();
        }
        Contact contact = new Contact(name, phoneNumber, uri);
        if (contactArrayList == null) {
            contactArrayList = new ArrayList<>();
        }
        contactArrayList.add(contact);
        contactListAdapter.notifyDataSetChanged();
        saveContactsListSharePref(this, contactArrayList);
    }
}