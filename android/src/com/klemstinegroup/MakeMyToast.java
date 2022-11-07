package com.klemstinegroup;

import android.app.Service;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.*;
import android.os.Process;
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


public class MakeMyToast extends Service {

    private PowerManager.WakeLock wakeLock;


    // This method run only one time. At the first time of service created and running
    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Log.d("onCreate()", "After service created");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Here is the source of the TOASTS :D
        Toast.makeText(this, "dreaming", Toast.LENGTH_SHORT).show();
        DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire(5 * 60 * 1000L /*10 minutes*/);
        System.out.println("getting image");
        SharedPreferences sharedPref = this.getSharedPreferences("prompts", Context.MODE_PRIVATE);
        String prompt=sharedPref.getString("prompts","");
        String[] prompts=prompt.split("\n");
        String pr=prompts[((int)(Math.random()*prompts.length))];
        System.out.println("loading:"+pr);
        getStableDiffusionImage(width, height, pr);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding
        return null;
    }

    public void getStableDiffusionImage(int width, int height, String prompt) {
        System.out.println(width + "," + height + "\t" + prompt);
        Pair<Integer, Integer> box = new Pair<>(width, height);
        Pair<Integer, Integer> bounds = new Pair<>(1024, 1024);
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
                        WallpaperManager wallpaperManager
                                = WallpaperManager.getInstance(getApplicationContext());
                        wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_SYSTEM);
                        wallpaperManager.setBitmap(bitmap, null, false, WallpaperManager.FLAG_LOCK);

                    }

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                wakeLock.release();
            }

            @Override
            public void failed(Throwable t) {
                System.out.println(t.getMessage());
                wakeLock.release();
            }

            @Override
            public void cancelled() {
                System.out.println("cancelled");
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