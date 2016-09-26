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
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.friendinneed.ua.friendinneed.model.Contact;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mymac on 9/22/16.
 */

public class SettingsActivity extends AppCompatActivity {

    public static final String SERVICE_ACTION = SettingsActivity.class.getSimpleName() + "_service";
    private Boolean trackingStatus = false;
    private SharedPreferences prefs;
    private SwitchCompat trackingSwitch;
    private TextView trackingSwitchTextView;
    private NumberPicker numberPicker;
    private Button addContactBtn;
    final int PICK_CONTACT = 1;
    private String name;
    private String phoneNumber;
    private ListView contactsAddedListView;
    private ContactListAdapter contactListAdapter;
    private static final int defaultTimeToWait = 15;
    private int timeToWait;

    private static final String SHARED_PREFS_NAME = "AddedContactsListShared";
    private static final String CONTACTS = "contacts";
    private static final String TRACKING_STATUS = "tracking_status";
    private static final String TIME_TO_WAIT = "time_to_wait";
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    public static final int PERMISSIONS_REQUEST_LOCATION = 200;
    public static final int PERMISSIONS_REQUEST_SMS = 300;
    public final static int SYSTEM_ALERTS_REQUEST_CODE = 5463 & 0xffffff00;

    public void checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, SYSTEM_ALERTS_REQUEST_CODE);
            }
        }
    }

    private Contact contact;
    private ArrayList<Contact> contactArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        prefs = getBaseContext().getSharedPreferences(SHARED_PREFS_NAME,
                Context.MODE_PRIVATE);

        contactArrayList = new ArrayList<>();

        addContactBtn = (Button) findViewById(R.id.add_contact_btn);
        addContactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readContact();
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
                if (trackingStatus) {
                    trackingStatus = false;
                }
                if (isChecked) {
                    startServiceIfPermissionsGranted();
                    trackingSwitchTextView.setText(getResources().getString(R.string.tracking_is_running));
                } else {
                    InneedService.stopInnedService(SettingsActivity.this);
                    trackingSwitchTextView.setText(getResources().getString(R.string.turn_on_tracking));
                }
                trackingStatus = isChecked;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(TRACKING_STATUS, isChecked);
                editor.commit();
            }
        });

        contactsAddedListView = (ListView) findViewById(R.id.contacts_added_list);
        trackingSwitch.setChecked(trackingStatus);

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
            case (PICK_CONTACT):
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    cursor.moveToFirst();
                    ContentResolver cr = getContentResolver();
                    String contactID = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                    // get phone number
                    if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

                        Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{contactID}, null);
                        while (pCur.moveToNext()) {
                            phoneNumber = pCur.getString(
                                    pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        }
                        pCur.close();
                    }

                    contact = new Contact(name, phoneNumber, uri);
                    if (contactArrayList == null) {
                        contactArrayList = new ArrayList<>();
                    }
                    contactArrayList.add(contact);
                    contactListAdapter.notifyDataSetChanged();

                    saveContactsListSharePref(this, contactArrayList);
                }
            case (SYSTEM_ALERTS_REQUEST_CODE):
            case (PERMISSIONS_REQUEST_LOCATION):
            case (PERMISSIONS_REQUEST_SMS):
                startServiceIfPermissionsGranted();
            default:
                //nothn
        }
    }

    private void startServiceIfPermissionsGranted() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        SettingsActivity.PERMISSIONS_REQUEST_LOCATION);
                return;
            }
            if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.SEND_SMS
                        },
                        SettingsActivity.PERMISSIONS_REQUEST_SMS);
                return;
            }
            if (!Settings.canDrawOverlays(this)) {
                checkDrawOverlayPermission();
                return;
            }
        }
        InneedService.startInneedService(this);
    }

    public void readContact() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
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
        } else
            return (ArrayList<Contact>) sharedContactList;

        return (ArrayList<Contact>) sharedContactList;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_CONTACTS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    startActivityForResult(intent, PICK_CONTACT);
                } else {
                    // Permission Denied
                    Toast.makeText(SettingsActivity.this, R.string.contacts_perm_accept, Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

}