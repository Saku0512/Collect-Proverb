package com.collectproverb.collectproverb;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;


import java.util.Calendar;

public class AlarmHelper {
    @SuppressLint("ScheduleExactAlarm")
    public static void setAlarm(Context context) {
        // 権限チェック
        if (!PermissionHelper.canScheduleExactAlarms(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionRequest.requestScheduleExactAlarmPermission(context);
            }
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.collectproverb.alarm.NOTIFY");
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? PendingIntent.FLAG_IMMUTABLE
                : 0;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                flags
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
}
