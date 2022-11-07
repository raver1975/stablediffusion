package com.klemstinegroup;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = this.getSharedPreferences("prompts", Context.MODE_MULTI_PROCESS);
        editor = sharedPref.edit();
        LinearLayout llPage = new LinearLayout(this);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {

                WallpaperManager wallpaperManager = WallpaperManager.getInstance(AndroidLauncher.this);
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    AndroidLauncher.this.requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 0);
                    return;
                }
                final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                getWindow().setBackgroundDrawable(wallpaperDrawable);
                new Handler(Looper.getMainLooper()).postDelayed(this, 30000);
            }


        });


        EditText editText = new EditText(this);
        Button refreshButton = new Button(this);
        EditText secondsText = new EditText(this);
        editText.setBackgroundColor(Color.parseColor("#88FFFFFF"));
        secondsText.setBackgroundColor(Color.parseColor("#88FFFFFF"));
        refreshButton.setBackgroundColor(Color.parseColor("#88FFFFFF"));

        secondsText.setInputType(InputType.TYPE_CLASS_NUMBER);
        secondsText.setSingleLine();
        secondsText.setText(30 * 60 + "");
        refreshButton.setText("refresh");
        llPage.addView(editText);
        llPage.addView(refreshButton);
        llPage.addView(secondsText);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlarmManager am = (AlarmManager) AndroidLauncher.this.getSystemService(Context.ALARM_SERVICE);
                Intent i = new Intent(AndroidLauncher.this, AlarmReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(AndroidLauncher.this, 0, i, 0);
                assert am != null;
                String[] splut = editText.getText().toString().split("\n");
                HashSet<String> hs = new HashSet<String>();
                for (String s : splut) {
                    hs.add(s);
                    Log.d("prompt", s);
                }
                editor.putStringSet("prompts", hs);
                editor.putInt("seconds", 60 * 30);
                try {
                    editor.putInt("seconds", Integer.parseInt(secondsText.getText().toString()));
                } catch (Exception e) {
                }
                editor.commit();
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() / 1000L + 2) * 1000L, pi); //Next alarm in 15s

            }
        });
        setContentView(llPage);

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
        editText.setText("stunning photograph of sunset");
        String[] splut = editText.getText().toString().split("\n");
        HashSet hs = new HashSet<String>();
        for (String s : splut) {
            hs.add(s);
        }
        editor.putStringSet("prompts", hs);
        editor.putInt("seconds", 60 * 30);
        try {
            editor.putInt("seconds", Integer.parseInt(secondsText.getText().toString()));
        } catch (Exception e) {
        }
        editor.commit();
        llPage.setOrientation(LinearLayout.VERTICAL);

//        AlarmReceiver alarm = new AlarmReceiver();
//        alarm.setAlarm(this);
        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        assert am != null;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() / 1000L + 1) * 1000L, pi); //Next alarm in 15s

    }
}