package com.br.octo.board;


import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_PREF_SYNC_CONN = "pref_syncConnectionType";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        setTitle(R.string.nav_settings);
        addPreferencesFromResource(R.xml.pref_general);
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);

        String stringValue = sharedPreferences.getString(key, "");

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);

            // Set the summary to reflect the new value.
            preference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);

            if (key.matches(getResources().getString(R.string.pref_key_language)))
            {
                switch (index) {
                    case 1:
                        LocaleHelper.setLocale(getBaseContext(), new Locale("pt", "BR"));
                        break;
                    case 2:
                        LocaleHelper.setLocale(getBaseContext(), new Locale("pt", "BR"));
                        break;
                    default:
                        LocaleHelper.setLocale(getBaseContext(), Locale.ENGLISH);
                        break;
                }
                restart_settings();
            }
            else if (listPreference.getKey().matches(getResources().getString(R.string.pref_key_sync_frequency)))
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


        if (key.equals(KEY_PREF_SYNC_CONN)) {
            Preference connectionPref = findPreference(key);
            // Set summary to be the user-description for the selected value
            connectionPref.setSummary(sharedPreferences.getString(key, ""));
        }
    }

    private void restart_settings()
    {
        setTitle(R.string.nav_settings);
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.pref_general);
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


//    private static Context settingContext;
//
//    private static FragmentManager act;
//
//
//    /**
//     * A preference value change listener that updates the preference's summary
//     * to reflect its new value.
//     */
//    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
//        @Override
//        public boolean onPreferenceChange(Preference preference, Object value) {
//            String stringValue = value.toString();
//
//            if (preference instanceof ListPreference) {
//                // For list preferences, look up the correct display value in
//                // the preference's 'entries' list.
//                ListPreference listPreference = (ListPreference) preference;
//                int index = listPreference.findIndexOfValue(stringValue);
//
//                // Set the summary to reflect the new value.
//                preference.setSummary(
//                        index >= 0
//                                ? listPreference.getEntries()[index]
//                                : null);
//
//                if (listPreference.getKey().matches(settingContext.getResources().getString(R.string.pref_key_language_frequency)))
//                {
//                    switch (index) {
//                        case 1:
//                            LocaleHelper.setLocale(settingContext, new Locale("pt", "BR"));
//                            break;
//                        case 2:
//                            LocaleHelper.setLocale(settingContext, new Locale("pt", "BR"));
//                            break;
//                        default:
//                            LocaleHelper.setLocale(settingContext, Locale.ENGLISH);
//                            break;
//                    }
//                }
//                else if (listPreference.getKey().matches(settingContext.getResources().getString(R.string.pref_key_sync_frequency)))
//                {
//                    if (index == 0)
//                    {
//                        // Show Warning of battery consumption!
//                    }
//                }
//            }
//            else {
//                // For all other preferences, set the summary to the value's
//                // simple string representation.
//                preference.setSummary(stringValue);
//            }
//            return true;
//        }
//    };
//
//    /**
//     * Helper method to determine if the device has an extra-large screen. For
//     * example, 10" tablets are extra-large.
//     */
//    private static boolean isXLargeTablet(Context context) {
//        return (context.getResources().getConfiguration().screenLayout
//                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
//    }
//
//    /**
//     * Binds a preference's summary to its value. More specifically, when the
//     * preference's value is changed, its summary (line of text below the
//     * preference title) is updated to reflect the value. The summary is also
//     * immediately updated upon calling this method. The exact display format is
//     * dependent on the type of preference.
//     *
//     * @see #sBindPreferenceSummaryToValueListener
//     */
//    private static void bindPreferenceSummaryToValue(Preference preference) {
//        // Set the listener to watch for value changes.
//        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
//
//        // Trigger the listener immediately with the preference's
//        // current value.
//        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
//                PreferenceManager
//                        .getDefaultSharedPreferences(preference.getContext())
//                        .getString(preference.getKey(), ""));
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setupActionBar();
//        setTitle(R.string.nav_settings);
//
//        getFragmentManager().beginTransaction()
//                .replace(android.R.id.content, new GeneralPreferenceFragment())
//                .commit();
//
//        act = getFragmentManager();
//        settingContext = getBaseContext();
//        setResult(RESULT_CANCELED);
//    }
//
//    /**
//     * Set up the {@link android.app.ActionBar}, if the API is available.
//     */
//    private void setupActionBar() {
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            // Show the Up button in the action bar.
//            actionBar.setDisplayHomeAsUpEnabled(true);
//        }
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public boolean onIsMultiPane() {
//        return isXLargeTablet(this);
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public void onBuildHeaders(List<Header> target) {
//        loadHeadersFromResource(R.xml.pref_headers, target);
//    }
//
//    /**
//     * This method stops fragment injection in malicious applications.
//     * Make sure to deny any unknown fragments here.
//     */
//    protected boolean isValidFragment(String fragmentName) {
//        //return PreferenceFragment.class.getName().equals(fragmentName)
//        return GeneralPreferenceFragment.class.getName().equals(fragmentName);  // ||
////                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
////                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
//    }
//
//    /**
//     * This fragment shows general preferences only. It is used when the
//     * activity is showing a two-pane settings UI.
//     */
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static class GeneralPreferenceFragment extends PreferenceFragment {
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.pref_general);
//            setHasOptionsMenu(true);
//
//            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
//            // to their values. When their values change, their summaries are
//            // updated to reflect the new value, per the Android Design
//            // guidelines.
//            //bindPreferenceSummaryToValue(findPreference("example_text"));
//            bindPreferenceSummaryToValue(findPreference("example_list"));
//            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
//        }
//    }
//
//    /**
//     * This fragment shows notification preferences only. It is used when the
//     * activity is showing a two-pane settings UI.
//     */
////    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
////    public static class NotificationPreferenceFragment extends PreferenceFragment {
////        @Override
////        public void onCreate(Bundle savedInstanceState) {
////            super.onCreate(savedInstanceState);
////            addPreferencesFromResource(R.xml.pref_notification);
////            setHasOptionsMenu(true);
////
////            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
////            // to their values. When their values change, their summaries are
////            // updated to reflect the new value, per the Android Design
////            // guidelines.
////            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
////        }
////    }
//
//    /**
//     * This fragment shows data and sync preferences only. It is used when the
//     * activity is showing a two-pane settings UI.
//     */
////    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
////    public static class DataSyncPreferenceFragment extends PreferenceFragment {
////        @Override
////        public void onCreate(Bundle savedInstanceState) {
////            super.onCreate(savedInstanceState);
////            addPreferencesFromResource(R.xml.pref_data_sync);
////            setHasOptionsMenu(true);
////
////            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
////            // to their values. When their values change, their summaries are
////            // updated to reflect the new value, per the Android Design
////            // guidelines.
////            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
////        }
////    }
//
//
//}
