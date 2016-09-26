package com.friendinneed.ua.friendinneed;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.friendinneed.ua.friendinneed.model.Contact;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mymac on 9/22/16.
 */

public class SettingsActivity extends AppCompatActivity {

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

        contactsAddedListView = (ListView) findViewById(R.id.contacts_added_list);


        trackingSwitchTextView = (TextView) findViewById(R.id.tracking_switch_text);
        trackingSwitch = (SwitchCompat) findViewById(R.id.tracking_switch);
        numberPicker = (NumberPicker) findViewById(R.id.numb_picker_settings);
        numberPicker.setWrapSelectorWheel(false);
        timeToWait = prefs.getInt(TIME_TO_WAIT, 15);

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

        trackingStatus = prefs.getBoolean(TRACKING_STATUS, false);
        trackingSwitch.setChecked(trackingStatus);

        trackingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (trackingStatus) {
                    trackingStatus = false;
                }
                if (isChecked) {
                    startServiceIfPermissionGranted();
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

        contactArrayList = getContactsListSharePref(this);
        if (contactArrayList == null) {
            contactArrayList = new ArrayList<>();
        }
        contactListAdapter = new ContactListAdapter(this, R.layout.contact_list_item, contactArrayList);
        contactsAddedListView.setAdapter(contactListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        trackingStatus = prefs.getBoolean(TRACKING_STATUS, false);
        trackingSwitch.setChecked(trackingStatus);


        timeToWait = prefs.getInt(TIME_TO_WAIT, 15);

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
                startServiceIfPermissionGranted();
            default:
                //nothn
        }
    }

    private void startServiceIfPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                checkDrawOverlayPermission();
            } else {
                InneedService.startInneedService(this);
            }
        } else {
            InneedService.startInneedService(this);
        }
    }

    public void readContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
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


    public ArrayList<Contact> getContactsListSharePref(Context context) {
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

}