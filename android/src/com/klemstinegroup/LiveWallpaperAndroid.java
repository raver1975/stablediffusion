package com.klemstinegroup;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;

import android.util.Pair;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.NetJavaImpl;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.*;

public class LiveWallpaperAndroid extends WallpaperService {

    private final int wait = 1000*60*60;

    @Override
    public Engine onCreateEngine() {

        return new Engine() {
            @Override
            public void onSurfaceChanged(SurfaceHolder holder,
                                         int format, int width, int height) {
                super.onSurfaceChanged(holder,format,width,height);
                final Handler handler = new Handler(Looper.myLooper());
                Engine engine = this;
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
//                        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
//                        int width = display.getWidth();
//                        int height = display.getHeight();
                        System.out.println("getting image");
                        getStableDiffusionImage(width, height, "stunning photograph of sunset over colorful vibrant lush tropical island in a beautiful blue sea, with lightning flashes in the background", this, handler);
                    }
                };
                if (!engine.isPreview()) handler.post(r);
            }
        };
    }

    public void getStableDiffusionImage(int width, int height, String prompt, Runnable runnable, Handler handler) {

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
                        handler.postDelayed(runnable, wait);
                    }

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    handler.postDelayed(runnable, wait);
                }
            }

            @Override
            public void failed(Throwable t) {
                handler.postDelayed(runnable, wait);
                System.out.println(t.getMessage());
            }

            @Override
            public void cancelled() {
                handler.postDelayed(runnable, wait);
                System.out.println("cancelled");
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