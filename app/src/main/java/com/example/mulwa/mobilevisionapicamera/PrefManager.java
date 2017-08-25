package com.example.mulwa.mobilevisionapicamera;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by mulwa on 8/24/17.
 */

public class PrefManager {
    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    // shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "welcome-page";

    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";

    public PrefManager(Context context) {
        this.context = context;

        sharedPreferences = context.getSharedPreferences(PREF_NAME,PRIVATE_MODE);
        editor  = sharedPreferences.edit();
    }
    public void setIsFirstTimeLaunch(boolean isFirstTimeLaunch){
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTimeLaunch);
        editor.commit();
    }
    public boolean isFirstTimeLaunch(){
        return sharedPreferences.getBoolean(IS_FIRST_TIME_LAUNCH,true);
    }
}
