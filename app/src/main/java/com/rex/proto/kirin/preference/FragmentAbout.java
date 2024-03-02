package com.rex.proto.kirin.preference;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.rex.proto.kirin.BuildConfig;
import com.rex.proto.kirin.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FragmentAbout extends PreferenceFragmentCompat {

    private static final Logger sLogger = LoggerFactory.getLogger(FragmentAbout.class);
    public static final String TAG = "ABOUT";

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preference_about, rootKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sLogger.trace("");
        Preference prefsVersion = getPreferenceScreen().findPreference("PREFS_VERSION");
        if (prefsVersion != null) {
            prefsVersion.setSummary(BuildConfig.VERSION_NAME);
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
            actionBar.setTitle(R.string.settings_about);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        sLogger.trace("");
    }
}
