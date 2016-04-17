package com.friendinneed.ua.friendinneed;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by mymac on 4/17/16.
 */
public class ContactsActivity extends Activity implements View.OnClickListener{

    public static final String PREFS_NAME = "PRODUCT_APP";
    private static final String CONTACTS_SET_KEY = "contacts2";


    SharedPreferences sPref;
    Button btnAddContact;
    EditText etEnterContactNumber;
    ListView lvContactNumbers;
    Set<String> contactsDefaultSet = new HashSet<String>();
    //    String PHONE_REGEX = "^\\+(?:[0-9]●?){6,14}[0-9]$";
    String PHONE_REGEX = "^((\\+\\d{1,3}(-| )?\\(?\\d\\)?(-| )?\\d{1,3})|(\\(?\\d{2,3}\\)?))(-| )?(\\d{3,4})(-| )?(\\d{4})(( x| ext)\\d{1,5}){0,1}$";
    Set<String> contactsStringSet;
    SharedPreferences.Editor ed;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_activity);

        etEnterContactNumber = (EditText) findViewById(R.id.etEnterContactNumber);
        btnAddContact = (Button) findViewById(R.id.btnAddContact);
        lvContactNumbers = (ListView) findViewById(R.id.lvContactNumbers);

        sPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        ed = sPref.edit();

        etEnterContactNumber.setHint("Enter valid number here");
        btnAddContact.setOnClickListener(this);
        showContact();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contacts_activity, menu);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void addContact(){



        String phoneNumber = etEnterContactNumber.getText().toString();
        SharedPrefsUtils.setStringPreference(this, CONTACTS_SET_KEY, phoneNumber);
//        phoneNumber = PhoneNumberUtils.formatNumber(phoneNumber);
//        Toast.makeText(this, phoneNumber, Toast.LENGTH_SHORT).show();
//        if(PhoneNumberUtils.isGlobalPhoneNumber(phoneNumber) && Pattern.matches(PHONE_REGEX, phoneNumber)){
//
//            contactsStringSet = sPref.getStringSet(CONTACTS_SET_KEY, contactsDefaultSet);
//            contactsStringSet.add(PhoneNumberUtils.formatNumber(phoneNumber));
//            ed.clear();
//            ed.putStringSet(CONTACTS_SET_KEY, contactsStringSet);
//            ed.commit();
//            etEnterContactNumber.setText("");
//            Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show();
//        } else {
//            etEnterContactNumber.setError("Please enter valide phone number");
//        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private Set<String> getContacts(){
        sPref = getSharedPreferences(PREFS_NAME ,MODE_PRIVATE);
        Set<String> contactsStringSet = sPref.getStringSet(CONTACTS_SET_KEY, contactsDefaultSet);

        return contactsStringSet;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnAddContact:
                Log.i("btn", "press btn");
                addContact();
                showContact();
                break;
        }
    }

    private void showContact(){
        Set<String> contactsFromSharedPref = getContacts();
        String[] contactsArray = contactsFromSharedPref.toArray(new String[contactsFromSharedPref.size()]);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, contactsArray);
        lvContactNumbers.setAdapter(adapter);
    }
}
