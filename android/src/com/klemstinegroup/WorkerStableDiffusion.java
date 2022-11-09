package com.klemstinegroup;

import android.app.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
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

import java.io.*;
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

    public void getStableDiffusionImage(int xwidth, int xheight, String prompt) {
//        flag = resetflag;
       /* int x = MAX_AI_HEIGHT;
        int y = MAX_AI_WIDTH;
        if (xheight < xwidth) {
            y = (MAX_AI_HEIGHT * xwidth) / xheight;
        } else {
            x = (MAX_AI_WIDTH * xwidth) / xheight;
        }
        x = (x / 64) * 64;
        y = (y / 64) * 64;*/
        int x=MAX_AI_WIDTH;
        int y=MAX_AI_HEIGHT;
        Log.d("prompt", "1asking for size:" + x + "," + y);
        Net.HttpRequest request = new Net.HttpRequest();
        request.setHeader("apikey", "xfuOtOK5sae3VGX60CJx1Q");
        request.setHeader("Content-Type", "application/json");
        request.setContent("{\"prompt\":\"" + prompt + "\", \"params\":{\"n\":1,\"use_gfpgan\": true, \"karras\": false, \"use_real_esrgan\": true, \"use_ldsr\": true, \"use_upscaling\": true, \"width\": " + x + ", \"height\": " + y + "}}");
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
                            String filename = "image-" + Math.abs(prompt.hashCode()) + "-" + ((int) (Math.random() * Integer.MAX_VALUE)) + ".png";
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

                      /*  //draw letterbox around ai
                        int x = MAX_AI_HEIGHT;
                        int y = MAX_AI_WIDTH;
                        if (xheight > xwidth) {
                            y = (MAX_AI_HEIGHT * xheight) / xwidth;
                        } else {
                            x = (MAX_AI_WIDTH * xheight) / xwidth;
                        }
                        Log.d("prompt", "xwidth:" + xwidth + "," + xheight);
                        Log.d("prompt", "asking for size:" + x + "," + y);
                        Bitmap dstBmp = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(dstBmp);
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        int dx = -(srcBmp.getWidth() - x) / 2;
                        int dy = -(srcBmp.getHeight() - y) / 2;
                        Log.d("prompt", "differential?" + dx + "," + dy);
                        canvas.drawBitmap(srcBmp, dx, dy, null);*/

///*                        //draw inset
                        int x = (MAX_AI_WIDTH * xwidth) / xheight;
                        int y = MAX_AI_HEIGHT;
                        Log.d("prompt", x + "," + y + "\t" + "crop");
                        Bitmap dstBmp1 = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
                        Canvas canvas1 = new Canvas(dstBmp1);
                        canvas1.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        int dx = -(srcBmp.getWidth() - x) / 2;
                        int dy = -(srcBmp.getHeight() - y) / 2;
                        Log.d("prompt", "differential?" + dx + "," + dy);
                        canvas1.drawBitmap(srcBmp, dx, dy, null);

                        //getInpainting(srcBmp, xwidth, xheight, prompt);

                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                        wallpaperManager.setBitmap(dstBmp1, null, false, WallpaperManager.FLAG_SYSTEM);
                        wallpaperManager.setBitmap(dstBmp1, null, false, WallpaperManager.FLAG_LOCK);
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


    public void getInpainting(Bitmap bitmap, int xwidth, int xheight, String prompt) {
        Log.d("prompt", "getting outpainted image");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, baos);
        byte[] b = baos.toByteArray();
        Log.d("prompt", "byte upload length:" + b.length);
        String imageEncoded = Base64.encodeToString(b, Base64.NO_WRAP);
        int x = 2*MAX_AI_HEIGHT;
        int y = 2*MAX_AI_WIDTH;
        if (xheight < xwidth) {
            y = (2*MAX_AI_HEIGHT * xwidth) / xheight;
        } else {
            x = (2*MAX_AI_WIDTH * xwidth) / xheight;
        }
        x = (x / 64) * 64;
        y = (y / 64) * 64;
        Log.d("prompt", "xwidth:" + xwidth + "," + xheight);
        Log.d("prompt", "asking for size:" + x + "," + y);
        Net.HttpRequest request = new Net.HttpRequest();
        request.setHeader("apikey", "xfuOtOK5sae3VGX60CJx1Q");
        request.setHeader("Content-Type", "application/json");
        String outt = "{\"models\":[\"stable_diffusion\"],\"prompt\":\"" + prompt + "\", \"params\":{\"n\":1,\"prompt\":\"" + prompt + "\",\"use_gfpgan\": true, \"karras\": false, \"use_real_esrgan\": true, \"use_ldsr\": true, \"use_upscaling\": true, \"width\": " + x + ", \"height\": " + y + "},\"source_processing\":\"img2img\",\"source_image\":\"" + imageEncoded + "\"}";
        Log.d("prompt", "out" + outt.substring(0, Math.min(500, outt.length())));
        request.setContent(outt);

//        request.setContent("{\"source_processing\":\"outpainting\",\"source_image\":\"" + imageEncoded + "\",\"prompt\":\"" + prompt + "\", \"params\":{\"n\":1,\"use_gfpgan\": true, \"karras\": false, \"use_real_esrgan\": true, \"use_ldsr\": true, \"use_upscaling\": true, \"width\": " + x + ", \"height\": " + y + "}}");
        request.setUrl("https://stablehorde.net/api/v2/generate/sync");
        request.setTimeOut(0);
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
                            String filename = "ximage-" + Math.abs(prompt.hashCode()) + "-" + ((int) (Math.random() * Integer.MAX_VALUE)) + ".png";
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

                       /* int x = MAX_AI_HEIGHT;
                        int y = MAX_AI_WIDTH;
                        if (xheight > xwidth) {
                            y = (MAX_AI_HEIGHT * xheight) / xwidth;
                        } else {
                            x = (MAX_AI_WIDTH * xheight) / xwidth;
                        }
                        Log.d("prompt", "xwidth:" + xwidth + "," + xheight);
                        Log.d("prompt", "creating size:" + x + "," + y);
                        Bitmap dstBmp = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);

                        Canvas canvas = new Canvas(dstBmp);
                        canvas.drawColor(Color.TRANSPARENT);
                        int dx = -(srcBmp.getWidth() - x) / 2;
                        int dy = -(srcBmp.getHeight() - y) / 2;
                        Log.d("prompt", "differential?" + dx + "," + dy);
                        canvas.drawBitmap(srcBmp, dx, dy, null);*/

/*                        int x = (2 * MAX_AI_WIDTH * xwidth) / xheight;
                        int y = 2 * MAX_AI_HEIGHT;
                        Log.d("prompt", x + "," + y + "\t" + "crop");
                        Bitmap dstBmp1 = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
                        Canvas canvas1 = new Canvas(dstBmp1);
                        canvas1.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        int dx = -(srcBmp.getWidth() - x) / 2;
                        int dy = -(srcBmp.getHeight() - y) / 2;
                        Log.d("prompt", "differential?" + dx + "," + dy);
                        canvas1.drawBitmap(srcBmp, dx, dy, null);*/

                        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                        wallpaperManager.setBitmap(srcBmp, null, false, WallpaperManager.FLAG_SYSTEM);
                        wallpaperManager.setBitmap(srcBmp, null, false, WallpaperManager.FLAG_LOCK);
                        done = true;
                    }

                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    Log.d("prompt", sw.toString());
                    done = true;
                }
            }

            @Override
            public void failed(Throwable t) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                Log.d("prompt", sw.toString());
                done = true;
            }

            @Override
            public void cancelled() {
                Log.d("prompt", "cancelled");
                done = true;
            }

        });
    }
}
