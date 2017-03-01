package com.quatro.octo.qu4tro;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;

/**
 * This class is used to change your application locale and persist this change for the next time
 * that your app is going to be used.
 * <p/>
 * You can also change the locale of your application on the fly by using the setLocale method.
 * <p/>
 * Created by gunhansancar on 07/10/15.
 */
public class LocaleHelper {

    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";

    public static Context onAttach(Context context) {
        String lang = getPersistedData(context, Locale.getDefault().getLanguage());
        if (lang.matches("pt_BR"))
        {
            return setLocale(context, new Locale("pt", "BR"));
        }
        else if (lang.matches("pt_PT"))
        {
            return setLocale(context, new Locale("pt", "PT"));
        }
        else
        {
            return setLocale(context, Locale.ENGLISH);
        }
    }

    public static Context onAttach(Context context, String defaultLanguage) {
        String lang = getPersistedData(context, defaultLanguage);

        if (lang.matches("pt_BR"))
        {
            return setLocale(context, new Locale("pt", "BR"));
        }
        else if (lang.matches("pt_PT"))
        {
            return setLocale(context, new Locale("pt", "PT"));
        }
        else
        {
            return setLocale(context, Locale.ENGLISH);
        }
    }

    public static String getLanguage(Context context) {
        return getPersistedData(context, Locale.getDefault().getLanguage());
    }

    public static Context setLocale(Context context, Locale language) {
        persist(context, language);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, language);
        }

        return updateResourcesLegacy(context, language);
    }

    public static Context setLocale(Context context, Locale language, Activity activity) {
        persist(context, language);
        Context returning_context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            returning_context= updateResources(context, language);
            activity.recreate();
            return returning_context;
        }

        returning_context = updateResourcesLegacy(context, language);
        activity.recreate();
        return returning_context;
    }

    private static String getPersistedData(Context context, String defaultLanguage) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(SELECTED_LANGUAGE, defaultLanguage);
    }

    private static void persist(Context context, Locale language) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(SELECTED_LANGUAGE, language.toString());
        editor.apply();
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(Context context, Locale language) {
        Locale.setDefault(language);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(language);

        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, Locale language) {
        Locale.setDefault(language);
        Resources resources = context.getResources();

        Configuration configuration = resources.getConfiguration();
        configuration.locale = language;

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        return context;
    }
}
