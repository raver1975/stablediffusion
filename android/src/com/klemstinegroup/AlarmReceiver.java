package com.klemstinegroup;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.NetJavaImpl;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashSet;

import static android.content.Context.POWER_SERVICE;

public class AlarmReceiver extends BroadcastReceiver{
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onReceive(Context context, Intent intent)
    {
//        Intent in = new Intent(context, MakeMyToast.class);
//        context.startService(in);
        setAlarm(context);
    }

    public void setAlarm(Context context)
    {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        assert am != null;


        SharedPreferences sharedPref = context.getSharedPreferences("prompts", Context.MODE_MULTI_PROCESS);
        int seconds=sharedPref.getInt("seconds",60*30);
        HashSet<String> prompt = (HashSet<String>) sharedPref.getStringSet("prompts",null);
        String[] array= prompt.toArray(new String[0]);
//        Collections.shuffle(array);
        String pr = array[((int) (Math.random() * (double) array.length))];
        //Here is the source of the TOASTS :D
//        Toast.makeText(context, "dreaming of " + pr, Toast.LENGTH_LONG).show();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyApp::MyWakelockTag");
        wakeLock.acquire(5 * 60 * 1000L /*10 minutes*/);
       Log.d("prompt","getting image");

        Log.d("prompt","loading:" + pr);
        getStableDiffusionImage(width, height, pr,context);

        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()/1000L + seconds) *1000L, pi); //Next alarm in 15s

    }
    public void getStableDiffusionImage(int width, int height, String prompt,Context context) {
        Log.d("get image",width + "," + height + "\t" + prompt);
        Pair<Integer, Integer> box = new Pair<>(width, height);
        Pair<Integer, Integer> bounds = new Pair<>(512, 512);
        Pair<Integer, Integer> constains = getScaledDimension(box, bounds);
//        int offset = (int) (height*.05);
//        height += offset;
        width = ((int) constains.first / 64) * 64;
        height = ((int) constains.second / 64) * 64;

//        flag = resetflag;
        Net.HttpRequest request = new Net.HttpRequest();
        request.setHeader("apikey", "0000000000");
        request.setHeader("Content-Type", "application/json");
        request.setContent("{\"prompt\":\""
                + prompt
                + "\", \"params\":{\"n\":1, \"width\": " + width + ", \"height\": " + height + "}}");
        request.setUrl("https://stablehorde.net/api/v2/generate/sync");
        request.setTimeOut(300000);
        request.setMethod("POST");
//        System.out.println("image", "getting image");

        new NetJavaImpl().sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String result = httpResponse.getResultAsString();
                Log.d("result", result);
                Log.d("prompt back",prompt);
//                Toast.makeText(context, "dreamt of " + prompt, Toast.LENGTH_LONG).show();
                System.out.println("stable diffusion response:" + result);
                try {
                    JsonReader reader = new JsonReader();
                    JsonValue resultJSON = reader.parse(result);
                    JsonValue generations = resultJSON.get("generations");
                    String imgData = generations.get(0).getString("img");
                    if (generations != null && imgData != null) {
                        byte[] bytes = Base64Coder.decode(imgData);
                        ByteArrayInputStream bas = new ByteArrayInputStream(bytes);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
                        wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM);
                        wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK);
                    }

                } catch (Exception e) {
                    Log.d("error",e.getMessage());
                }
                wakeLock.release();
            }

            @Override
            public void failed(Throwable t) {
                Log.d("error",t.getMessage());
                wakeLock.release();
            }

            @Override
            public void cancelled() {
                Log.d("error","cancelled");
                wakeLock.release();
            }
        });
    }

    public static Pair<Integer, Integer> getScaledDimension(Pair<Integer, Integer> imgSize, Pair<Integer, Integer> boundary) {

        int original_width = imgSize.first;
        int original_height = imgSize.second;
        int bound_width = boundary.first;
        int bound_height = boundary.second;
        int new_width = original_width;
        int new_height = original_height;

        // first check if we need to scale width
        if (original_width > bound_width) {
            //scale width to fit
            new_width = bound_width;
            //scale height to maintain aspect ratio
            new_height = (new_width * original_height) / original_width;
        }

        // then check if we need to scale even with the new height
        if (new_height > bound_height) {
            //scale height to fit instead
            new_height = bound_height;
            //scale width to maintain aspect ratio
            new_width = (new_height * original_width) / original_height;
        }

        return new Pair(new_width, new_height);
    }
}