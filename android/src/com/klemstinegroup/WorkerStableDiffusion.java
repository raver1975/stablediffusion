package com.klemstinegroup;

import android.app.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.*;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.NetJavaImpl;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class WorkerStableDiffusion extends Worker {
    private static final int MAX_AI_WIDTH = 512;
    private static final int MAX_AI_HEIGHT = 512;
    boolean done = false;
    private SharedPreferences sharedPref;

    public WorkerStableDiffusion(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public ListenableWorker.Result doWork() {

        // Do the work here--in this case, upload the images.
        String progress = "Starting Download";
        setForegroundAsync(createForegroundInfo(progress));
        SharedPreferences sharedPref = this.getApplicationContext().getSharedPreferences("prompts", Context.MODE_MULTI_PROCESS);
        WorkRequest wr1 = new OneTimeWorkRequest.Builder(WorkerStableDiffusion.class).setInitialDelay(sharedPref.getInt("seconds", 60 * 30), TimeUnit.SECONDS).build();
        WorkManager.getInstance(getApplicationContext()).enqueue(wr1);

        uploadImages();
        long timeThen = System.currentTimeMillis() + 360000l;

        while (!done && System.currentTimeMillis() < timeThen) {
        }

        // Indicate whether the work finished successfully with the Result
        return ListenableWorker.Result.success();
    }

    private void uploadImages() {
        sharedPref = this.getApplicationContext().getSharedPreferences("prompts", Context.MODE_MULTI_PROCESS);
        HashSet<String> prompt = (HashSet<String>) sharedPref.getStringSet("prompts", null);
        String[] array = prompt.toArray(new String[0]);
        String pr = array[((int) (Math.random() * (double) array.length))];
        DisplayMetrics metrics = this.getApplicationContext().getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        Log.d("prompt", "loading:" + pr);
        getStableDiffusionImage(width, height, pr);
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(@NonNull String progress) {
        // Build a notification using bytesRead and contentLength

        Context context = getApplicationContext();
//        String id = context.getString(R.string.notification_channel_id);
//        String title = context.getString(R.string.notification_title);
//        String cancel = context.getString(R.string.cancel_download);
        // This PendingIntent can be used to cancel the worker
        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(getId());

        createChannel();

        Notification notification = new Notification.Builder(context, "download")
                .setContentTitle("Diffuse")
                .setTicker("Diffuse")

                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                // Add the cancel action to the notification which can
                // be used to cancel the worker
                .addAction(android.R.drawable.ic_delete, "delete", intent)
                .build();

        return new ForegroundInfo(1, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = getApplicationContext().getString(R.string.app_name);
        String description = "download";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel("download", name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    /*public void getStableDiffusionPrompt(int width, int height, String prompt) {
        Net.HttpRequest request = new Net.HttpRequest();
        request.setHeader("apikey", "0000000000");
        request.setHeader("Content-Type", "application/json");
        request.setContent("{\"prompt\":\"" + prompt + "\", \"params\":{\"n\":1}}");
        request.setUrl("https://koboldai.net/api/v2/generate/sync");
        request.setTimeOut(300000);
        request.setMethod("POST");

        new NetJavaImpl().sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String result = httpResponse.getResultAsString();
                Log.d("prompt", result);
//                Log.d("prompt back", prompt);
                JsonReader reader = new JsonReader();
                JsonValue resultJSON = reader.parse(result);
                JsonValue generations = resultJSON.get("generations");
                String tztData = generations.get(0).getString("text");
                getStableDiffusionImage(width, height, tztData);
            }

            @Override
            public void failed(Throwable t) {
                Log.d("error", t.getMessage());
                done = true;
            }

            @Override
            public void cancelled() {
                Log.d("error", "cancelled");
                done = true;
            }

        });
    }*/


    public void getStableDiffusionImage(int xwidth, int xheight, String prompt) {
//        flag = resetflag;
        Net.HttpRequest request = new Net.HttpRequest();
        request.setHeader("apikey", "0000000000");
        request.setHeader("Content-Type", "application/json");
        request.setContent("{\"prompt\":\"" + prompt + "\", \"params\":{\"n\":1,\"use_gfpgan\": true, \"karras\": false, \"use_real_esrgan\": true, \"use_ldsr\": true, \"use_upscaling\": true, \"width\": " + MAX_AI_WIDTH + ", \"height\": " + MAX_AI_HEIGHT + "}}");
        request.setUrl("https://stablehorde.net/api/v2/generate/sync");
        request.setTimeOut(300000);
        request.setMethod("POST");
//        System.out.println("image", "getting image");

        new NetJavaImpl().sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String result = httpResponse.getResultAsString();
                Log.d("prompt", "result" + result.substring(0, Math.min(200, result.length())));
//                Toast.makeText(context, "dreamt of " + prompt, Toast.LENGTH_LONG).show();
                try {
                    JsonReader reader = new JsonReader();
                    JsonValue resultJSON = reader.parse(result);
                    JsonValue generations = resultJSON.get("generations");
                    String imgData = generations.get(0).getString("img");
                    if (generations != null && imgData != null) {
                        byte[] bytes = Base64Coder.decode(imgData);
                        Bitmap srcBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                        if (sharedPref.getBoolean("savecheck", false)) {
                            String filename = "image-" + prompt.hashCode() + "-" + ((int) (Math.random() * Integer.MAX_VALUE)) + ".png";
                            File sd = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
                            File dest = new File(sd, filename);
                            try {
                                FileOutputStream out = new FileOutputStream(dest);
                                srcBmp.compress(Bitmap.CompressFormat.PNG, 90, out);
                                out.flush();
                                out.close();
                                Log.d("prompt", filename);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        //draw inboxed aspect ratio of ai image
                        int x = (MAX_AI_WIDTH * xwidth) / xheight;
                        int y = MAX_AI_HEIGHT;
                        Log.d("prompt", x + "," + y + "\t" + "crop");
                        Bitmap dstBmp = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);


/*                        //draw letterbox around ai
                        int x = MAX_AI_HEIGHT;
                        int y = MAX_AI_WIDTH;
                        if (xheight > xwidth) {
                            y = (MAX_AI_HEIGHT * xheight) / xwidth;
                        } else {
                            x = (MAX_AI_WIDTH * xheight) / xwidth;
                        }
                        Log.d("prompt", "xwidth:" + xwidth + "," + xheight);
                        Log.d("prompt", "asking for size:" + x + "," + y);
                        Bitmap dstBmp = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);*/

                        Canvas canvas = new Canvas(dstBmp);
                        canvas.drawColor(Color.TRANSPARENT);
                        int dx = -(srcBmp.getWidth() - x) / 2;
                        int dy = -(srcBmp.getHeight() - y) / 2;
                        Log.d("prompt", "differential?" + dx + "," + dy);
                        canvas.drawBitmap(srcBmp, dx, dy, null);
                        String filename = "work.webp";
                        File sd = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
                        File dest = new File(sd, filename);
                        try {
                            FileOutputStream out = new FileOutputStream(dest);
                            dstBmp.compress(Bitmap.CompressFormat.WEBP, 90, out);
                            out.flush();
                            out.close();
                            Log.d("prompt", filename);
//                            WorkRequest wr = new OneTimeWorkRequest.Builder(WorkerSuperScale.class).build();
//                            WorkManager.getInstance(getApplicationContext()).enqueue(wr);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                        wallpaperManager.setBitmap(dstBmp, null, false, WallpaperManager.FLAG_SYSTEM);
                        wallpaperManager.setBitmap(dstBmp, null, false, WallpaperManager.FLAG_LOCK);
                        done = true;
                    }

                } catch (Exception e) {
                    Log.d("prompt", e.getMessage());
                    done = true;
                }
            }

            @Override
            public void failed(Throwable t) {
                Log.d("prompt", t.getMessage());
                done = true;
            }

            @Override
            public void cancelled() {
                Log.d("prompt", "cancelled");
                done = true;
            }

        });
    }

    public static Pair<Integer, Integer> getScaledDimension
            (Pair<Integer, Integer> imgSize, Pair<Integer, Integer> boundary) {

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