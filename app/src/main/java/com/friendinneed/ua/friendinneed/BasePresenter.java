package com.friendinneed.ua.friendinneed;


public abstract class BasePresenter<T extends BaseView> {

  protected T view;

  void takeView(T baseView) {
    this.view = baseView;
    onTakeView(baseView);
  }

  protected abstract void onTakeView(T view);


}
