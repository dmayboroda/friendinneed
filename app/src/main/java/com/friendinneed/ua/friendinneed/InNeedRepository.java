package com.friendinneed.ua.friendinneed;

import com.friendinneed.ua.friendinneed.model.DataSampleRequest;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;


@Singleton
public class InNeedRepository implements Repository {
  private final InNeedApi inNeedApi;

  @Inject
  public InNeedRepository(InNeedApi inNeedApi) {
    this.inNeedApi = inNeedApi;
  }

  @Override public Call<String> getFriendInNeedDbContent() {
    return inNeedApi.getFriendInNeedDbContent();
  }

  @Override public Call<String> saveSampleDataLabeled(DataSampleRequest dataSampleRequest) {
    return inNeedApi.saveSampleDataLabeled(dataSampleRequest);
  }

  @Override public Call<String> deleteFriendInNeedDbContent() {
    return inNeedApi.deleteFriendInNeedDbContent();
  }

  @Override public Call<String> retrainFriendInNeed() {
    return inNeedApi.retrainFriendInNeed();
  }

  @Override public Call<String> retrainSplitFriendInNeed() {
    return inNeedApi.retrainSplitFriendInNeed();
  }

  @Override public Call<String> generateCsv() {
    return inNeedApi.generateCsv();
  }

  @Override public Call<String> generateTestCsv() {
    return inNeedApi.generateTestCsv();
  }

  @Override public Call<String> generateTrainCsv() {
    return inNeedApi.generateTrainCsv();
  }

  @Override public Call<String> checkFall(DataSampleRequest dataSampleRequest) {
    return inNeedApi.checkFall(dataSampleRequest);
  }
}
