package com.klemstinegroup;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

public class AndroidLauncher extends Activity {

    private static final int STORAGE_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout llPage = new LinearLayout(this);
        llPage.setOrientation(LinearLayout.VERTICAL);
//        AlarmReceiver alarm = new AlarmReceiver();
//        alarm.setAlarm(this);
        AlarmManager am =( AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(getApplicationContext(), AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, i, 0);
        assert am != null;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()/1000L + 1) *1000L, pi); //Next alarm in 15s
        this.moveTaskToBack(true);
    }
}