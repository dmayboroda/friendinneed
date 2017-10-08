package com.friendinneed.ua.friendinneed;

import android.app.Application;
import com.facebook.stetho.Stetho;
import com.friendinneed.ua.friendinneed.di.AppComponent;
import com.friendinneed.ua.friendinneed.di.DaggerAppComponent;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * Created by skozyrev on 10/8/17.
 */

public class FriendApp extends Application {

  private static FriendApp instance;
  private AppComponent appComponent;

  @Inject
  String baseUrl;

  public static FriendApp getApp() {
    return instance;
  }

  public static AppComponent getComponent() {
    return getApp().appComponent;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;
    initDevTools();
    appComponent = DaggerAppComponent.builder().application(this)
        .build();
    appComponent.inject(this);
  }

  private void initDevTools() {

    Stetho.initializeWithDefaults(this);
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    }
  }
}
