package com.friendinneed.ua.friendinneed.di;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AppExecutors {
  private final Executor ioExecutor;
  private final Executor networkExecutor;
  private final Executor mainThreadExecutor;

  private AppExecutors(Executor ioExecutor, Executor networkExecutor, Executor mainThreadExecutor){
    this.ioExecutor = ioExecutor;
    this.networkExecutor = networkExecutor;
    this.mainThreadExecutor = mainThreadExecutor;
  }

  @Inject
  public AppExecutors(){
    this(Executors.newSingleThreadExecutor(), Executors.newFixedThreadPool(5), new MainThreadExecutor());
  }

  public Executor getIoExecutor() {
    return ioExecutor;
  }

  public Executor getNetworkExecutor() {
    return networkExecutor;
  }

  public Executor getMainThreadExecutor() {
    return mainThreadExecutor;
  }

  private static class MainThreadExecutor implements Executor {
    Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    @Override public void execute(@NonNull Runnable command) {
      mainThreadHandler.post(command);
    }
  }
}
