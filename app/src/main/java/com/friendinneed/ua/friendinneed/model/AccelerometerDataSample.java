package com.friendinneed.ua.friendinneed.model;

import com.google.gson.annotations.Expose;

public class AccelerometerDataSample extends DataSample {
    @Expose
    private float accX;
    @Expose
    private float accY;
    @Expose
    private float accZ;

    public AccelerometerDataSample(float accX, float accY, float accZ) {
        super();
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
    }

    public float getAccX() {
        return accX;
    }

    public void setAccX(float accX) {
        this.accX = accX;
    }

    public float getAccY() {
        return accY;
    }

    public void setAccY(float accY) {
        this.accY = accY;
    }

    public float getAccZ() {
        return accZ;
    }

    public void setAccZ(float accZ) {
        this.accZ = accZ;
    }

    @Override
    public int getDataType() {
        return DATA_SAMPLE_TYPE_ACCELEROMETER;
    }

    public static AccelerometerDataSample fromDataSample(DataSample dataSample) {
        return (AccelerometerDataSample) dataSample;
    }

    @Override
    public String toString() {
        return "accX=" + accX + ", accY=" + accY + ", accZ=" + accZ;
    }
}
