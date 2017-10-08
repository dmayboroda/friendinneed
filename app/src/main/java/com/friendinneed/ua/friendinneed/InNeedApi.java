package com.friendinneed.ua.friendinneed;

import com.friendinneed.ua.friendinneed.model.DataSampleRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface InNeedApi {
  @GET("/")
  Call<String> getFriendInNeedDbContent();

  @POST("/")
  Call<String> saveSampleDataLabeled(@Body DataSampleRequest dataSampleRequest);

  @GET("/delete_all")
  Call<String> deleteFriendInNeedDbContent();

  @GET("/retrain")
  Call<String> retrainFriendInNeed();

  @GET("/retrain_split")
  Call<String> retrainSplitFriendInNeed();

  @GET("/generate_csv")
  Call<String> generateCsv();

  @GET("/generate_test_csv")
  Call<String> generateTestCsv();

  @GET("/generate_train_csv")
  Call<String> generateTrainCsv();

  @POST("/check_fall")
  Call<String> checkFall(@Body DataSampleRequest dataSampleRequest);
}
