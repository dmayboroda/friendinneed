package com.friendinneed.ua.friendinneed;

import com.friendinneed.ua.friendinneed.model.DataSampleRequest;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Created by skozyrev on 10/8/17.
 */

public interface Repository {

  Call<String> getFriendInNeedDbContent();

  Call<String> saveSampleDataLabeled(DataSampleRequest dataSampleRequest);

  Call<String> deleteFriendInNeedDbContent();

  Call<String> retrainFriendInNeed();

  Call<String> retrainSplitFriendInNeed();

  Call<String> generateCsv();

  Call<String> generateTestCsv();

  Call<String> generateTrainCsv();

  Call<String> checkFall(DataSampleRequest dataSampleRequest);
}
