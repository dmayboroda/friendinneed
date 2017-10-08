package com.friendinneed.ua.friendinneed.di;

import android.app.Application;
import com.friendinneed.ua.friendinneed.FriendApp;
import com.friendinneed.ua.friendinneed.InNeedService;
import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;

/**
 * Created by skozyrev on 10/8/17.
 */
@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {

  void inject(FriendApp friendApp);

  void inject(InNeedService inNeedService);

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder application(Application application);

    AppComponent build();
  }
}
