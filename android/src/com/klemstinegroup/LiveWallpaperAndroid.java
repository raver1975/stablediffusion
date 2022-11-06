package com.klemstinegroup;

import android.util.Log;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService;
import com.badlogic.gdx.backends.android.AndroidWallpaperListener;

public class LiveWallpaperAndroid extends AndroidLiveWallpaperService { 
 
 @Override
 public void onCreateApplication () {
  super.onCreateApplication();
  AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
//  config.useGL20 = false;
  config.useCompass = false;
  config.useWakelock = false;
  config.useAccelerometer = false;
  config.getTouchEventsForLiveWallpaper = true;
  
  ApplicationListener listener = new LiveWallpaperStarter();
  initialize(listener, config);
 }

}