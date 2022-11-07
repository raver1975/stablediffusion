package com.klemstinegroup;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;

public class AndroidLauncher extends Activity {
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    String[] prompts = new String[]{"stunning photograph of sunset over beautiful blue sea, with lightning flashes in the background", "stunning photograph of aurora borealis and starry night sky over a tropical beach island"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref= this.getSharedPreferences("prompts", Context.MODE_PRIVATE);
        editor=sharedPref.edit();
        super.onCreate(savedInstanceState);
        LinearLayout llPage = new LinearLayout(this);
        EditText editText=new EditText(this);
        String st="";
        setContentView(llPage);
        for (String s:prompts){
            st=st+s+"\n";
        }
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                editor.putString("prompts",s.toString());
                editor.apply();

            }
        });
        editText.setText(st);
        llPage.setOrientation(LinearLayout.VERTICAL);
        llPage.addView(editText);
//        AlarmReceiver alarm = new AlarmReceiver();
//        alarm.setAlarm(this);
        AlarmManager am =( AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        assert am != null;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()/1000L + 1) *1000L, pi); //Next alarm in 15s

    }
}