package com.br.octo.board.modules.settings;


import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.view.WindowManager;

import com.br.octo.board.R;
import com.br.octo.board.modules.base.AppCompatPreferenceActivity;
import com.br.octo.board.modules.base.BaseActivity;

import java.util.Locale;

/**
 * Created by Endy.
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    Resources res;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        setTitle(R.string.nav_settings);
        addPreferencesFromResource(R.xml.pref_general);

        res = getResources();
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

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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

            if (key.matches(res.getString(R.string.pref_key_language)))
            {
                switch (index) {
                    case 1:
                        LocaleHelper.setLocale(getBaseContext(), new Locale("pt", "BR"));
                        break;
                    case 2:
                        LocaleHelper.setLocale(getBaseContext(), new Locale("pt", "BR"));   //PT
                        break;
                    default:
                        LocaleHelper.setLocale(getBaseContext(), Locale.ENGLISH);
                        break;
                }

                // Restarting the Title and the preferences widgets to use the selected language
                setTitle(R.string.nav_settings);
                setPreferenceScreen(null);
                addPreferencesFromResource(R.xml.pref_general);

                setResult(RESULT_OK);
            }
            else if (listPreference.getKey().matches(res.getString(R.string.pref_key_sync_frequency)))
            {
                if (index == 0)
                {
                    // Show Warning of battery consumption!
                }
            }
        }
        else if (preference instanceof SwitchPreference) {

            // If it matches the Keep Screen Key
            if (key.matches(res.getString(R.string.pref_key_keep_screen)))
            {
                if (sharedPreferences.getBoolean(key, false)) {
                    setResult(RESULT_FIRST_USER);
                    BaseActivity.keepScreen = true;
                    AppCompatPreferenceActivity.keepScreen = true;
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                else {
                    setResult(RESULT_CANCELED);
                    BaseActivity.keepScreen = false;
                    AppCompatPreferenceActivity.keepScreen = false;
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        }
    }
}
