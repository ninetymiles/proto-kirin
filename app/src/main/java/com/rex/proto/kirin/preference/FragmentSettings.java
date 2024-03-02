package com.rex.proto.kirin.preference;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.rex.proto.kirin.BuildConfig;
import com.rex.proto.kirin.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FragmentSettings extends PreferenceFragmentCompat {

    private static final Logger sLogger = LoggerFactory.getLogger(FragmentSettings.class);

    public static final String TAG = "GENERAL";

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preference_settings, rootKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sLogger.trace("");

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        Preference prefsVersion = getPreferenceScreen().findPreference("PREFS_VERSION");
        if (prefsVersion != null) {
            prefsVersion.setSummary(BuildConfig.VERSION_NAME);
        }

        Preference prefsAbout = getPreferenceScreen().findPreference("PREFS_ABOUT");
        if (prefsAbout != null) {
            prefsAbout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull Preference preference) {
                    sLogger.trace("PREFS_ABOUT");
                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.preferenceContent, new FragmentAbout())
                            .addToBackStack(null)
                            .commit();
                    return true;
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sLogger.trace("");
    }

    @Override
    public void onStart() {
        super.onStart();
        sLogger.trace("");

        // Need specified the title, PreferenceFragmentCompat will not auto apply from PreferenceScreen title
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.menu_settings);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        sLogger.trace("");
    }
}
