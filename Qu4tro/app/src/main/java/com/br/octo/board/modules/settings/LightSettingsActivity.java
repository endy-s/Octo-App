package com.br.octo.board.modules.settings;


import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;

import com.br.octo.board.R;
import com.br.octo.board.modules.base.AppCompatPreferenceActivity;

/**
 * Created by Endy.
 */
public class LightSettingsActivity extends AppCompatPreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    Resources res;
    SharedPreferences sharedLightPref;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        setTitle(R.string.title_activity_light_settings);
        addPreferencesFromResource(R.xml.pref_light);

        res = getResources();

        sharedLightPref = getPreferenceScreen().getSharedPreferences();

        ListPreference modePreference = (ListPreference) findPreference(res.
                getString(R.string.pref_key_light_mode));
        setFreqEnabled(modePreference.findIndexOfValue(sharedLightPref.
                getString(res.getString(R.string.pref_key_light_mode), "")));
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedLightPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedLightPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(sharedPreferences.getString(key, ""));

            // Set the summary to reflect the new value.
            preference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);

            if (key.matches(res.getString(R.string.pref_key_light_mode)))
            {
                setFreqEnabled(index);
            }
            else if (listPreference.getKey().matches(res.getString(R.string.pref_key_light_freq)))
            {
                if (index == 0)
                {
                    // Show Warning of battery consumption!
                }
            }
        }
        else {
            // For all other preferences, set the summary to the value's
            // simple string representation.
//            preference.setSummary(stringValue);
        }

//        if (key.equals(KEY_PREF_SYNC_CONN)) {
//            Preference connectionPref = findPreference(key);
//            // Set summary to be the user-description for the selected value
//            connectionPref.setSummary(sharedPreferences.getString(key, ""));
//        }

    }

    // Set the Light Frequency Enabled status
    // Disabled if the Mode is "Always On"
    // Enabled if the Mode is "Fade" or "Strobe"
    public void setFreqEnabled(int index) {
        Preference preference_freq = findPreference(res.getString(R.string.pref_key_light_freq));

        if (index == 0) {
            preference_freq.setEnabled(false);
        }
        else {
            preference_freq.setEnabled(true);
        }
    }
}
