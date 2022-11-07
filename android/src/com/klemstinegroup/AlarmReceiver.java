package com.klemstinegroup;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class AlarmReceiver extends BroadcastReceiver{
    int secondsdelay=60*30;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent in = new Intent(context, MakeMyToast.class);
        context.startService(in);
        setAlarm(context);
    }

    public void setAlarm(Context context)
    {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        assert am != null;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()/1000L + secondsdelay) *1000L, pi); //Next alarm in 15s

    }
}