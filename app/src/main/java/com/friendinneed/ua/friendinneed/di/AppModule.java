package com.friendinneed.ua.friendinneed.di;

import android.app.Application;
import android.content.Context;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.friendinneed.ua.friendinneed.BuildConfig;
import com.friendinneed.ua.friendinneed.R;
import dagger.Module;
import dagger.Provides;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by skozyrev on 10/8/17.
 */
@Module
class AppModule {

  private static final long READ_TIMEOUT = 60;
  private static final long CONNECTION_TIMEOUT = 60;

  @Provides
  @Singleton
  Context provideContext(Application application) {
    return application.getApplicationContext();
  }

  @Provides
  String provideBaseUrl(Context context) {
    return context.getString(R.string.api_url);
  }

  @Provides
  @Singleton
  OkHttpClient provideOkHttpClient(List<Interceptor> interceptors,
      @NetworkInterceptors List<Interceptor> networkInterceptors) {
    final OkHttpClient.Builder builder = new OkHttpClient.Builder();
    for (Interceptor interceptor : interceptors) {
      builder.addInterceptor(interceptor);
    }
    if (BuildConfig.DEBUG) {
      for (Interceptor interceptor : networkInterceptors) {
        builder.addNetworkInterceptor(interceptor);
      }
    }
    builder.readTimeout(READ_TIMEOUT, TimeUnit.SECONDS);
    builder.connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS);
    return builder.build();
  }


  @Provides
  @NetworkInterceptors
  List<Interceptor> provideNetworkInterceptors() {
    return Collections.<Interceptor>singletonList(new StethoInterceptor());
  }

  @Provides
  HttpLoggingInterceptor provideHttpLoggingInterceptor() {
    HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
    interceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY :
        HttpLoggingInterceptor.Level.NONE);
    return interceptor;
  }
  @Provides
  List<Interceptor> provideInterceptors(HttpLoggingInterceptor httpLoggingInterceptor) {
    return Arrays.<Interceptor>asList(httpLoggingInterceptor);
  }


}
