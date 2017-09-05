package com.br.octo.board.modules.settings;


import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;

import com.br.octo.board.R;
import com.br.octo.board.Variables;
import com.br.octo.board.api_services.BluetoothHelper;
import com.br.octo.board.models.SeekBarPreference;
import com.br.octo.board.modules.base.AppCompatPreferenceActivity;

/**
 * Created by Endy.
 */
public class LightSettingsActivity extends AppCompatPreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener, BluetoothHelper.BluetoothCallback {

    Resources res;
    SharedPreferences sharedLightPref;

    SwitchPreference enablePreference;
    ListPreference modePreference;
    SeekBarPreference intensityPreference, thresholdPreference;

    BluetoothHelper btHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_light);
        setupActionBar();
        addPreferencesFromResource(R.xml.pref_light);

        res = getResources();

        sharedLightPref = getPreferenceScreen().getSharedPreferences();

        enablePreference = (SwitchPreference) findPreference(res.getString(R.string.pref_key_light_enabled));
        modePreference = (ListPreference) findPreference(res.getString(R.string.pref_key_light_mode));
        setFreqEnabled(modePreference.findIndexOfValue(sharedLightPref.getString(res.getString(R.string.pref_key_light_mode), "")));
        if (Variables.lowPowerMode) modePreference.setEnabled(false);

        intensityPreference = (SeekBarPreference) this.findPreference(res.getString(R.string.pref_key_light_intensity));
        thresholdPreference = (SeekBarPreference) this.findPreference(res.getString(R.string.pref_key_light_threshold));
        setSeekBars();

        btHelper = BluetoothHelper.getInstance();
        btHelper.setInLightScreenFlag(true);
        btHelper.setCallback(this);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.title_activity_light_settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedLightPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        btHelper.setInLightScreenFlag(false);
        sharedLightPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);

        if (Variables.lowPowerMode) modePreference.setEnabled(false);

        String newStateMsg = "<W=1;";

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(sharedPreferences.getString(key, ""));

            // Set the summary to reflect the new value.
            listPreference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

            if (key.matches(res.getString(R.string.pref_key_light_mode))) {
                setFreqEnabled(index);

                newStateMsg += "L=" + listPreference.getValue() + ";>";
            } else if (key.matches(res.getString(R.string.pref_key_light_freq))) {
                newStateMsg += "F=" + listPreference.getValue() + ";>";
            }
        } else if (preference instanceof SwitchPreference) {
            SwitchPreference switchPreference = (SwitchPreference) preference;

            if (key.matches(res.getString(R.string.pref_key_light_enabled))) {
                newStateMsg += "L=" + (switchPreference.isChecked() ? modePreference.getValue() : "0") + ";>";
            }
        } else if (preference instanceof SeekBarPreference) {
            int radius = 0;

            if (key.matches(res.getString(R.string.pref_key_light_intensity))) {
                newStateMsg += "I=";

                radius = sharedLightPref.getInt(res.getString(R.string.pref_key_light_intensity), 50);
                preference.setSummary(this.getString(R.string.pref_light_intensity_summary).replace("$1", "" + radius));
            } else if (key.matches(res.getString(R.string.pref_key_light_threshold))) {
                newStateMsg += "P=";

                radius = sharedLightPref.getInt(res.getString(R.string.pref_key_light_threshold), 10);
                preference.setSummary(this.getString(R.string.pref_light_threshold_summary).replace("$1", "" + radius));
            }

            if (radius == 100) radius = 99;
            newStateMsg += String.format("%02d", radius) + ";>";
        }

        if (btHelper.getConnectionStatus()) {
            btHelper.sendMessage(newStateMsg);
        }
    }


    public void setFreqEnabled(int index) {
        Preference preference_freq = findPreference(res.getString(R.string.pref_key_light_freq));
        preference_freq.setEnabled(index != 0);
    }

    public void setSeekBars() {
        // Set seekbar properties:
        int radius = sharedLightPref.getInt(res.getString(R.string.pref_key_light_intensity), 50);
        intensityPreference.setSummary(this.getString(R.string.pref_light_intensity_summary).replace("$1", "" + radius));
        intensityPreference.setMin(1);
        radius = sharedLightPref.getInt(res.getString(R.string.pref_key_light_threshold), 10);
        thresholdPreference.setSummary(this.getString(R.string.pref_light_threshold_summary).replace("$1", "" + radius));
        thresholdPreference.setMin(10);
    }

    @Override
    public void onMessageReceived(String message) {
        if (message.startsWith("U")) {
            final String finalMessage = message;
            final int value = Integer.valueOf(message.split(";")[1].substring(2));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (finalMessage.contains("L=")) {
                        updateLightState(value);
                    } else if (finalMessage.contains("P=")) {
                        setLowBattMode(value);
                    }
                }
            });
        }
    }

    @Override
    public void onDeviceConnected() {
    }

    @Override
    public void onDeviceDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LightSettingsActivity.this.showDisconnectedDialog();
            }
        });
    }

    private void showDisconnectedDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_disconnect_title))
                .setMessage(getString(R.string.dialog_disconnect_light_message))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LightSettingsActivity.this.finish();
                    }
                })
                .show();
    }

    private void updateLightState(int newState) {
        sharedLightPref.unregisterOnSharedPreferenceChangeListener(this);

        if (newState == 0) {
            if (enablePreference.isChecked()) {
                enablePreference.setChecked(false);
            }
        } else {
            if (!enablePreference.isChecked()) {
                enablePreference.setChecked(true);
            }

            if (!modePreference.getValue().equals(String.valueOf(newState))) {
                modePreference.setValue(String.valueOf(newState));
                int index = modePreference.findIndexOfValue("1");
                modePreference.setSummary(index >= 0 ? modePreference.getEntries()[index] : null);
                setFreqEnabled(index);
            }
        }

        sharedLightPref.registerOnSharedPreferenceChangeListener(this);
    }

    private void setLowBattMode(int lowBattMode) {
        Variables.lowPowerMode = (lowBattMode != 0);

        sharedLightPref.unregisterOnSharedPreferenceChangeListener(this);

        modePreference.setEnabled(!Variables.lowPowerMode);

        if (!enablePreference.isChecked()) {
            enablePreference.setChecked(true);
        }

        if (intensityPreference.getProgress() != 50) {
            SharedPreferences.Editor prefEditor = sharedLightPref.edit();

            intensityPreference.setProgress(50);
            intensityPreference.setSummary(this.getString(R.string.pref_light_intensity_summary).replace("$1", "" + 50));

            prefEditor.putInt(res.getString(R.string.pref_key_light_intensity), 50);
            prefEditor.apply();
        }

        if (!modePreference.getValue().equals("1")) {
            modePreference.setValue("1");
            int index = modePreference.findIndexOfValue("1");
            modePreference.setSummary(index >= 0 ? modePreference.getEntries()[index] : null);
            setFreqEnabled(index);
        }

        sharedLightPref.registerOnSharedPreferenceChangeListener(this);
    }
}