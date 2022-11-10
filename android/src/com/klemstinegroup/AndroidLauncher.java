package com.klemstinegroup;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.work.*;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

public class AndroidLauncher extends Activity {
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    Bitmap wallpaper;

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v("permission", "Permission is granted");
        } else {
            Log.v("permission", "Permission is revoked");
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        this.getActionBar().hide();
        sharedPref = this.getSharedPreferences("prompts", Context.MODE_MULTI_PROCESS);
        int bbb = sharedPref.getInt("seconds", 60 * 30);
        WorkRequest wr = new OneTimeWorkRequest.Builder(WorkerStableDiffusion.class).build();
        WorkManager.getInstance(getApplicationContext()).cancelAllWork();
        WorkManager.getInstance(getApplicationContext()).enqueue(wr);

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
                wallpaper = drawableToBitmap(wallpaperDrawable);
                getWindow().setBackgroundDrawable(wallpaperDrawable);
                new Handler(Looper.getMainLooper()).postDelayed(this, 15000);
            }
        });


        EditText editText = new EditText(this);
        Button resetButton = new Button(this);
        Button shareButton = new Button(this);
        Button hideButton = new Button(this);
        EditText secondsText = new EditText(this);
        CheckBox saveCheckbox = new CheckBox(this);
        editText.setBackgroundColor(Color.parseColor("#88FFFFFF"));
        secondsText.setBackgroundColor(Color.parseColor("#88FFFFFF"));
        resetButton.setBackgroundColor(Color.parseColor("#88FFFFFF"));
        shareButton.setBackgroundColor(Color.parseColor("#88FFFFFF"));
        saveCheckbox.setBackgroundColor(Color.parseColor("#88FFFFFF"));
        hideButton.setBackgroundColor(Color.parseColor("#88FFFFFF"));
        shareButton.setText("Share");
        secondsText.setText(bbb + "");
        hideButton.setText("hide");
        saveCheckbox.setChecked(sharedPref.getBoolean("savecheck", false));
        hideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                llPage.setVisibility(View.GONE);
            }
        });
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                WallpaperManager wallpaperManager = WallpaperManager.getInstance(AndroidLauncher.this);
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    AndroidLauncher.this.requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 0);
                    return;
                }
                final Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                getWindow().setBackgroundDrawable(wallpaperDrawable);

                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/jpeg");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                wallpaper.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), wallpaper, "Title", null);
                Uri imageUri = Uri.parse(path);
                share.putExtra(Intent.EXTRA_STREAM, imageUri);
                startActivity(Intent.createChooser(share, "Select"));
            }
        });
        secondsText.setInputType(InputType.TYPE_CLASS_NUMBER);
        secondsText.setSingleLine();

        resetButton.setText("Save/Reset");
        llPage.addView(editText);
        llPage.addView(saveCheckbox);
        llPage.addView(secondsText);
        llPage.addView(resetButton);
        llPage.addView(shareButton);
        llPage.addView(hideButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] splut = editText.getText().toString().split("\n");
                HashSet<String> hs = new HashSet<String>();
                for (String s : splut) {
                    hs.add(s);
                    Log.d("prompt", s);
                }
                editor.putStringSet("prompts", hs);
                int bbb1 = 30 * 60;
                try {
                    bbb1 = Integer.parseInt(secondsText.getText().toString());
                } catch (Exception e) {
                }
                editor.putInt("seconds", bbb1);
                editor.putBoolean("savecheck", saveCheckbox.isChecked());
                editor.commit();
                WorkRequest wr1 = new OneTimeWorkRequest.Builder(WorkerStableDiffusion.class).build();
                WorkManager.getInstance(getApplicationContext()).cancelAllWork();
                WorkManager.getInstance(getApplicationContext()).enqueue(wr1);
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
        Set<String> hs = sharedPref.getStringSet("prompts", null);
        if (hs == null) {
            editText.setText("stunning photograph of sunset");
        } else {
            String b = "";
            for (String s : hs) {
                b = b + s + "\n";
            }
            b = b.substring(0, b.length() - 1);
            editText.setText(b);
        }
        String[] splut = editText.getText().toString().split("\n");
        HashSet hs1 = new HashSet<String>();
        for (String s : splut) {
            hs1.add(s);
        }
        editor.putStringSet("prompts", hs1);
        bbb = 60 * 30;
        try {
            bbb = Integer.parseInt(secondsText.getText().toString());
        } catch (Exception e) {
        }
        editor.putInt("seconds", bbb);
        editor.commit();
        llPage.setOrientation(LinearLayout.VERTICAL);

//        AlarmReceiver alarm = new AlarmReceiver();
//        alarm.setAlarm(this);
//        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
//        Intent i = new Intent(this, AlarmReceiver.class);
//        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
//        assert am != null;
//        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() / 1000L + 1) * 1000L, pi); //Next alarm in 15s

    }

    public static String encodeToBase64(Bitmap image) {
        Bitmap immage = image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immage.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);

        Log.d("Image Log:", imageEncoded);
        return imageEncoded;
    }
}