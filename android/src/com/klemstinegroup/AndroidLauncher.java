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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.HashSet;
import java.util.Set;

public class AndroidLauncher extends Activity {
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    String[] prompts = new String[]{"stunning photograph of sunset"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = this.getSharedPreferences("prompts", Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        super.onCreate(savedInstanceState);
        LinearLayout llPage = new LinearLayout(this);
        EditText editText = new EditText(this);
        Button refreshButton = new Button(this);
        refreshButton.setText("refresh");
        llPage.addView(refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                        AlarmManager am = (AlarmManager) AndroidLauncher.this.getSystemService(Context.ALARM_SERVICE);
                        Intent i = new Intent(AndroidLauncher.this, AlarmReceiver.class);
                        PendingIntent pi = PendingIntent.getBroadcast(AndroidLauncher.this, 0, i, 0);
                        assert am != null;
                        String[] splut=editText.getText().toString().split("\n");
                        HashSet<String> hs=new HashSet<String>();
                        for (String s:splut){
                            hs.add(s);
                            Log.d("prompt",s);
                        }
                        editor.putStringSet("prompts",hs );
                        editor.commit();
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() / 1000L + 2) * 1000L, pi); //Next alarm in 15s

            }
        });
        String st = "";
        setContentView(llPage);
        for (String s : prompts) {
            st = st + s + "\n";
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


            }
        });
        editText.setText(st);
        String[] splut=editText.getText().toString().split("\n");
        HashSet hs=new HashSet<String>();
        for (String s:splut){
            hs.add(s);
        }
        editor.putStringSet("prompts",hs );
        editor.commit();
        llPage.setOrientation(LinearLayout.VERTICAL);
        llPage.addView(editText);
//        AlarmReceiver alarm = new AlarmReceiver();
//        alarm.setAlarm(this);
        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        assert am != null;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() / 1000L + 1) * 1000L, pi); //Next alarm in 15s

    }
}