package com.friendinneed.ua.friendinneed.model;

import com.friendinneed.ua.friendinneed.BuildConfig;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

import static com.friendinneed.ua.friendinneed.model.DataSample.DATA_SAMPLE_TYPE_ACCELEROMETER;
import static com.friendinneed.ua.friendinneed.model.DataSample.DATA_SAMPLE_TYPE_GYROSCOPE;

public class DataSampleRequest {

    @Expose
    private List<AccelerometerDataSample> acc = new ArrayList<>();
    @Expose
    private List<GyroscopeDataSample> gyro = new ArrayList<>();
    @Expose
    private Integer label;
    @Expose
    private Integer version;

    public DataSampleRequest(DataSample[] dataSamples) {
        label = 0;
        version = BuildConfig.VERSION_CODE;
        for (DataSample sample : dataSamples) {
            switch (sample.getDataType()) {
                case DATA_SAMPLE_TYPE_ACCELEROMETER:
                    acc.add((AccelerometerDataSample) sample);
                    break;
                case DATA_SAMPLE_TYPE_GYROSCOPE:
                    gyro.add((GyroscopeDataSample) sample);
                    break;
            }
        }
    }

    public List<AccelerometerDataSample> getAcc() {
        return acc;
    }

    public void setAcc(List<AccelerometerDataSample> acc) {
        this.acc = acc;
    }

    public DataSampleRequest withAcc(List<AccelerometerDataSample> acc) {
        this.acc = acc;
        return this;
    }

    public List<GyroscopeDataSample> getGyro() {
        return gyro;
    }

    public void setGyro(List<GyroscopeDataSample> gyro) {
        this.gyro = gyro;
    }

    public DataSampleRequest withGyro(List<GyroscopeDataSample> gyro) {
        this.gyro = gyro;
        return this;
    }

    public Integer getLabel() {
        return label;
    }

    public void setLabel(Integer label) {
        this.label = label;
    }

    public DataSampleRequest withLabel(Integer label) {
        this.label = label;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
