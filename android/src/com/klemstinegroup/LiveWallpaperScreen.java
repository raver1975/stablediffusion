package com.klemstinegroup;

import android.util.Pair;
import com.aspose.imaging.Image;
import com.aspose.imaging.fileformats.png.PngColorType;
import com.aspose.imaging.imageoptions.JpegOptions;
import com.aspose.imaging.imageoptions.PngOptions;
import com.aspose.imaging.internal.aS.P;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class LiveWallpaperScreen implements Screen {
    private Texture texture;
    Game game;

    OrthographicCamera camera;
    //Texture textureBg;
    //TextureRegion background;
    SpriteBatch batcher;
    private int flag = 1;
    private int resetflag = 2000;
//    static byte[] bytes;

    public LiveWallpaperScreen(final Game game) {
        this.game = game;

        camera = new OrthographicCamera(320, 480);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);

//        textureBg = new Texture("badlogic.jpg");
//        textureBg.setFilter(TextureFilter.Linear, TextureFilter.Linear);
//        textureBg.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
//        background = new TextureRegion(textureBg, 0, 0, textureBg.getWidth(), textureBg.getHeight());
        Pixmap pixmap = new Pixmap(Gdx.files.internal("badlogic.jpg"));
//        Pixmap pixmap = new Pixmap(Gdx.files.external("test.png"));

        texture = new Texture(pixmap);
        batcher = new SpriteBatch();
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

    @Override
    public void hide() {
        // TODO Auto-generated method stub

    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub

    }

    private void draw(float delta) {
        GL20 gl = Gdx.gl;
        gl.glClearColor(0, 0, 0, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        batcher.setProjectionMatrix(camera.combined);
        batcher.begin();
        batcher.draw(texture, 0, 0, camera.viewportWidth, camera.viewportHeight);
        if (flag-- == 0) {
            getStableDiffusionImage(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), "sunset on a tropical island beach");
        }
        batcher.end();
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

    public void getStableDiffusionImage(int width, int height, String prompt) {

//        width = 256;
//        height = 256;
        double aspectratio = width / height;
        Pair<Integer, Integer> box = new Pair<>(width, height);
        Pair<Integer, Integer> bounds = new Pair<>(1024, 1024);
        Pair<Integer, Integer> constains = getScaledDimension(box, bounds);
        int offset=64;
        height+=offset;
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
        request.setTimeOut(0);
        request.setMethod("POST");
        Gdx.app.log("image", "getting image");

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String result = httpResponse.getResultAsString();
                Gdx.app.log("stable diffusion response:", result);
                try {
                    JsonReader reader = new JsonReader();
                    JsonValue resultJSON = reader.parse(result);
                    JsonValue generations = resultJSON.get("generations");
                    String imgData = generations.get(0).getString("img");
                    if (generations != null && imgData != null) {
//                                        Gdx.app.log("stable diffusion response", imgData.replaceAll("(.{80})", "$1\n"));
//                        String url = "data:image/png;base64," + imgData;
                        try {
                            byte[] bytes = Base64Coder.decode(imgData);
                            ByteArrayInputStream bas = new ByteArrayInputStream(bytes);
                            Image image = Image.load(bas);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            PngOptions options = new PngOptions();
//                            options.setColorType(PngColorType.TruecolorWithAlpha);
//                            com.aspose.imaging.License license = new com.aspose.imaging.License();
//                            license.setLicense();
                            image.save(baos, options);
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    Gdx.files.external("test.png").write(new ByteArrayInputStream(baos.toByteArray()), false);
//                            Gdx.app.log("log",new String(bytes));
//                            bytes=baos.toByteArray();
                                    Pixmap pixmap = new Pixmap(Gdx.files.external("test.png"));
                                    Pixmap watermark=new Pixmap(pixmap.getWidth(),pixmap.getHeight()-offset, Pixmap.Format.RGB888);
                                    watermark.drawPixmap(pixmap,0,-offset);
//                            Gdx.app.log("pixmap download", pixmap.getWidth() + "," + pixmap.getHeight());
//                            texture.dispose();
//                            textureBg.dispose();
                                    texture = new Texture(watermark);
                                    watermark.dispose();
                                    pixmap.dispose();
//                            texture.
//                            textureBg.setFilter(TextureFilter.Linear, TextureFilter.Linear);
//                            textureBg.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
//                            background = new TextureRegion(textureBg, 0, 0, pixmap.getWidth(), pixmap.getHeight());
//                           texture.draw(pixmap,0,0);
                                    flag = 10;
                                }
                            });


                        } catch (Exception e) {
                            Gdx.app.log("error", e.getMessage(), e);
                            flag = resetflag;
                        }

                    }

                } catch (Exception e) {
                    Gdx.app.log("error", e.getMessage(), e);
                    flag = resetflag;
                }
            }

            @Override
            public void failed(Throwable t) {
                flag = resetflag;
                Gdx.app.log("error", t.getMessage(), t);
            }

            @Override
            public void cancelled() {
                flag = resetflag;
                Gdx.app.log("error", "cancelled");
            }
        });
    }


    private void update(float delta) {
    }

    @Override
    public void render(float delta) {
        update(delta);
        draw(delta);
    }

    @Override
    public void resize(int width, int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub

    }

    @Override
    public void show() {
        // TODO Auto-generated method stub

    }
}