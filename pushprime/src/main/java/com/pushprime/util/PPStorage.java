package com.pushprime.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.pushprime.PushPrime;

/**
 * Created by PushPrime on 10/10/2016.
 */

/**
 * Handles Internal Storage of PushPrime SDK
 */
public class PPStorage {

    public static String FCM_TOKEN = "fcm_token";
    public static String PUSHPRIME_ID = "pushprime_id";
    public static String PREVIOUS_HEARTBEAT = "previous_heartbeat";

    public void save(String key, String value){
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(PushPrime.sharedHandler().applicationContext);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        if(key != null && value != null){
            edit.putString(key, value);
        }
        edit.apply();
    }

    public void saveLong(String key, long value){
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(PushPrime.sharedHandler().applicationContext);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        if(key != null){
            edit.putLong(key, value);
        }
        edit.apply();
    }

    public String get(String key, String defaultValue){
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(PushPrime.sharedHandler().applicationContext);
        if(sharedPreferences.contains(key)){
            return sharedPreferences.getString(key, defaultValue);
        }else{
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue){
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(PushPrime.sharedHandler().applicationContext);
        if(sharedPreferences.contains(key)){
            return sharedPreferences.getLong(key, defaultValue);
        }else{
            return defaultValue;
        }
    }
}
