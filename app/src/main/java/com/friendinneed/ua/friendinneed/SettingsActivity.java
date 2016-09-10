package com.friendinneed.ua.friendinneed;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Created by mymac on 8/7/16.
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    Preference timerPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_general);

        timerPreference = findPreference(getString(R.string.timer_key));
        updateTimerUi();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(getString(R.string.timer_key))) {
            updateTimerUi();
        }
    }

    private void updateTimerUi() {
        timerPreference.setTitle(String.format(getString(R.string.timer_pref_title),
                PreferenceManager.getDefaultSharedPreferences(this).
                        getInt(getString(R.string.timer_key), 15)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

}
