package com.friendinneed.ua.friendinneed;

/**
 * Created by skozyrev on 10/8/17.
 */

public interface InNeedContract {
  interface View extends BaseView{
    void onCheckFallSuccess(String result);
    void onCheckFallFailure(Throwable t);
    void onSendFallSuccess(String result);
    void onSendFallFailure(Throwable t);
  }

}
