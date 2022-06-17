package com.example.app_central;

import android.content.Context;
import android.content.SharedPreferences;

public class ManagePreference {

    private static ManagePreference managePreference;
    private final SharedPreferences sharedPreferences;

    public static ManagePreference getInstance(Context context) {
        if (managePreference == null) {
            managePreference = new ManagePreference(context);
        }
        return managePreference;
    }

    private ManagePreference(Context context) {
        sharedPreferences = context.getSharedPreferences("YourCustomNamedPreference", Context.MODE_PRIVATE);
    }

    public void saveDataBoolean(String key, Boolean value) {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putBoolean(key, value);
        prefsEditor.apply();
    }

    public Boolean getDataBoolean(String key) {
        if (sharedPreferences != null) {
            return sharedPreferences.getBoolean(key, false);
        }
        return false;
    }
}
