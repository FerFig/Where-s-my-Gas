package com.udacity.ferfig.wheresmygas.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.udacity.ferfig.wheresmygas.R;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener{
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
       addPreferencesFromResource(R.xml.wheres_my_gas_settings);

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        int count = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference p = preferenceScreen.getPreference(i);
            if (p instanceof ListPreference) {
                String val = sharedPreferences.getString(p.getKey(), "");
                setPrefSummary(p, val);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference currentPref =  findPreference(key);
        if (currentPref!=null){
            if (currentPref instanceof ListPreference){
                String desc = sharedPreferences.getString(currentPref.getKey(), "");
                setPrefSummary(currentPref, desc);
            }
        }
    }

    private void setPrefSummary(Preference preference, String value) {
        if (preference instanceof ListPreference){
            ListPreference listPreference = (ListPreference)preference;
            int preIdx = listPreference.findIndexOfValue(value);
            if (preIdx>=0){
                listPreference.setSummary(listPreference.getEntries()[preIdx]);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

}
