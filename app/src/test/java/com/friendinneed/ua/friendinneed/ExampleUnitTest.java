package com.friendinneed.ua.friendinneed;

import android.content.Context;
import com.friendinneed.ua.friendinneed.di.AppExecutors;
import com.friendinneed.ua.friendinneed.model.DataSampleRequest;
import java.io.IOException;
import java.util.concurrent.Callable;
import okhttp3.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExampleUnitTest {

  private static final String FAKE_STRING = "HELLO WORLD url";

  @Mock
  Context mMockContext;
  private InNeedContract.View view;
  private InNeedPresenter inNeedPresenter;
  private InNeedRepository repository;

  @Before
  public void setup() {
    repository = mock(InNeedRepository.class);
    view = mock(InNeedContract.View.class);
    inNeedPresenter = spy(new InNeedPresenter(repository, new AppExecutors()));
  }

  @Test
  public void readStringFromContext_LocalizedString() {
    // setup
    //when(mMockContext.getString(R.string.api_url))
    //    .thenReturn(FAKE_STRING);
    DataSampleRequest dataSampleRequest = new DataSampleRequest(null, 1);
    // when
    inNeedPresenter.sendTestFallData(dataSampleRequest);
    // then
    //assertThat(result, is(FAKE_STRING));
    verify(view).onSendFallSuccess(anyString());
  }
}