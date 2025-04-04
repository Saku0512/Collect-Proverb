package com.collectproverb.collectproverb;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

public class PermissionRequest {
    @RequiresApi(api = Build.VERSION_CODES.S)
    public static void requestScheduleExactAlarmPermission(Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        context.startActivity(intent);
    }
}
