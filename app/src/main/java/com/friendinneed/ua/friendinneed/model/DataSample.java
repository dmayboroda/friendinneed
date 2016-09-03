package com.friendinneed.ua.friendinneed.model;

import android.support.annotation.IntDef;

import com.google.gson.annotations.Expose;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class DataSample {

    @Expose
    private long timestamp;

    DataSample() {
        this.timestamp = System.currentTimeMillis();
    }

    public abstract @DataSampleType int getDataType();

    public long getTimestamp() {
        return timestamp;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DATA_SAMPLE_TYPE_ACCELEROMETER, DATA_SAMPLE_TYPE_GYROSCOPE})
    @interface DataSampleType {
    }

    public static final int DATA_SAMPLE_TYPE_ACCELEROMETER = 0;
    public static final int DATA_SAMPLE_TYPE_GYROSCOPE = 1;
}
