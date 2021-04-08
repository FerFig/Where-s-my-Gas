package com.ferfig.wheresmygas.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.ferfig.wheresmygas.R;

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

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(getString(R.string.pref_send_me_an_email))){
            sendEmailToMe();
        }
        return super.onPreferenceTreeClick(preference);
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

    private void sendEmailToMe(){
        String mVersionName="1.0", mVersionCode="1";
        PackageInfo localPackageInfo;
        try {
            localPackageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            mVersionName = localPackageInfo.versionName;
            mVersionCode = String.valueOf(localPackageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Intent email = new Intent(Intent.ACTION_SEND);
        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"ferfig.apps@gmail.com"});
        email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) +" ("+mVersionName+" ["+mVersionCode+"])");
        email.putExtra(Intent.EXTRA_TEXT, "\n\n\n(version: "+mVersionName+" ["+mVersionCode+"])");
        // Use email client only
        email.setType("message/rfc822");
        startActivity(Intent.createChooser(email, getString(R.string.choose_email_client)));
    }
}
