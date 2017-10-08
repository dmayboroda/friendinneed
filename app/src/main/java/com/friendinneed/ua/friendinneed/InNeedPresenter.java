package com.friendinneed.ua.friendinneed;

import com.friendinneed.ua.friendinneed.di.AppExecutors;
import com.friendinneed.ua.friendinneed.model.DataSampleRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;

/**
 * Created by skozyrev on 10/8/17.
 */

public class InNeedPresenter extends BasePresenter<InNeedContract.View> {

  private final InNeedRepository inNeedRepository;
  private final AppExecutors appExecutors;

  public InNeedPresenter(InNeedRepository inNeedRepository, AppExecutors appExecutors) {
    this.inNeedRepository = inNeedRepository;
    this.appExecutors = appExecutors;
  }

  private void executeInBackground(final Call call, final Callback callback) {
    appExecutors.getNetworkExecutor().execute(new Runnable() {
      @Override public void run() {
        call.enqueue(new Callback() {
          @Override public void onResponse(final Call call, final Response response) {
            appExecutors.getMainThreadExecutor().execute(new Runnable() {
              @Override public void run() {
                callback.onResponse(call, response);
              }
            });
          }

          @Override public void onFailure(final Call call, final Throwable t) {
            appExecutors.getMainThreadExecutor().execute(new Runnable() {
              @Override public void run() {
                callback.onFailure(call, t);
              }
            });
          }
        });
      }
    });
  }

  @Override protected void onTakeView(InNeedContract.View view) {

  }

  public void sendTestFallData(DataSampleRequest dataSampleRequest) {
    executeInBackground(inNeedRepository.saveSampleDataLabeled(dataSampleRequest), new Callback<String>() {
      @Override public void onResponse(Call<String> call, retrofit2.Response<String> response) {
        if (response.code() == 200) {
          view.onSendFallSuccess(response.body());
        } else {
          view.onSendFallFailure(new HttpException(response));
        }
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        view.onSendFallFailure(t);
      }
    });
  }

  public void checkFall(DataSampleRequest dataSampleRequest) {
    executeInBackground(inNeedRepository.checkFall(dataSampleRequest), new Callback<String>() {
      @Override public void onResponse(Call<String> call, retrofit2.Response<String> response) {
        if (response.code() == 200) {
          view.onCheckFallSuccess(response.body());
        } else {
          view.onCheckFallFailure(new HttpException(response));
        }
      }

      @Override public void onFailure(Call<String> call, Throwable t) {
        view.onCheckFallFailure(t);
      }
    });
  }
}
