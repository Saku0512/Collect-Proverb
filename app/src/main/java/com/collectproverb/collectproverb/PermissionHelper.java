package com.collectproverb.collectproverb;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Build;

public class PermissionHelper {
    public static boolean canScheduleExactAlarms(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return false;
    }
}
