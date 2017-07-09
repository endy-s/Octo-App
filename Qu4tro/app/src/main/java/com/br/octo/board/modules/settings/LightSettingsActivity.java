package com.br.octo.board.modules.settings;


import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;

import com.br.octo.board.R;
import com.br.octo.board.api_services.BluetoothHelper;
import com.br.octo.board.models.SeekBarPreference;
import com.br.octo.board.modules.base.AppCompatPreferenceActivity;

/**
 * Created by Endy.
 */
public class LightSettingsActivity extends AppCompatPreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    Resources res;
    SharedPreferences sharedLightPref;

    BluetoothHelper btHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_light);
        setupActionBar();
        setTitle(R.string.title_activity_light_settings);
        addPreferencesFromResource(R.xml.pref_light);

        res = getResources();

        sharedLightPref = getPreferenceScreen().getSharedPreferences();

        ListPreference modePreference = (ListPreference) findPreference(res.
                getString(R.string.pref_key_light_mode));
        ListPreference freqPreference = (ListPreference) findPreference(res.getString(R.string.pref_key_light_freq));

        // Get widgets :
        SeekBarPreference intensityPreference = (SeekBarPreference) this.findPreference(res.getString(R.string.pref_key_light_intensity));

        // Set listener :
//        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        // Set seekbar summary :
        int radius = sharedLightPref.getInt(res.getString(R.string.pref_key_light_intensity), 50);
        intensityPreference.setSummary(this.getString(R.string.pref_light_intensity_summary).replace("$1", "" + radius));


        setFreqEnabled(modePreference.findIndexOfValue(sharedLightPref.getString(res.getString(R.string.pref_key_light_mode), "")));

        modePreference.setEntryValues(res.getStringArray(R.array.pref_light_list_values));
        freqPreference.setEntryValues(res.getStringArray(R.array.pref_light_frequency_values));

        btHelper = BluetoothHelper.getInstance();
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

        String newStateMsg = "<W=1;";

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(sharedPreferences.getString(key, ""));

            // Set the summary to reflect the new value.
            listPreference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);

            if (key.matches(res.getString(R.string.pref_key_light_mode))) {
                setFreqEnabled(index);

                newStateMsg += "L=" + listPreference.getValue() + ";>";
            } else if (key.matches(res.getString(R.string.pref_key_light_freq))) {
                newStateMsg += "F=" + listPreference.getValue() + ";>";
                // TODO: check if there's current change at the board. If yes, show a warning
            }
        } else if (preference instanceof SwitchPreference) {
            SwitchPreference switchPreference = (SwitchPreference) preference;

            if (key.matches(res.getString(R.string.pref_key_light_enabled))) {
                newStateMsg += "L=";

                if (switchPreference.isChecked()) {
                    ListPreference lastState = (ListPreference) findPreference(res.getString(R.string.pref_key_light_mode));
                    newStateMsg += lastState.getValue() + ";>";
                } else {
                    newStateMsg += "0;>";
                }
            }
        } else if (preference instanceof SeekBarPreference) {
            newStateMsg += "I=";

            if (key.matches(res.getString(R.string.pref_key_light_intensity))) {
                int radius = sharedLightPref.getInt(res.getString(R.string.pref_key_light_intensity), 50);
                preference.setSummary(this.getString(R.string.pref_light_intensity_summary).replace("$1", "" + radius));
                newStateMsg += radius + ";>";
            }
        }

        if (btHelper.getConnectionStatus()) {
            btHelper.sendMessage(newStateMsg);
        }
    }

    public void setFreqEnabled(int index) {
        Preference preference_freq = findPreference(res.getString(R.string.pref_key_light_freq));

        if (index == 0) {
            preference_freq.setEnabled(false);
        } else {
            preference_freq.setEnabled(true);
        }
    }
}