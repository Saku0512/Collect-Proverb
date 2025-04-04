package com.collectproverb.collectproverb;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesUtil {
    private static final String PREF_NAME = "AppPreferences";
    private static final String KEY_IS_FIRST = "isFirst";

    // 初回起動かどうかを判定
    public static boolean isFirstLaunch(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_IS_FIRST, true);
    }

    // 初回起動フラグを更新
    public static void setFirstLaunch(Context context, boolean isFirst) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_FIRST, isFirst);
        editor.apply();
    }
}
