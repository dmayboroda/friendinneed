package com.friendinneed.ua.friendinneed.model;

import com.google.gson.annotations.Expose;


public class GyroscopeDataSample extends DataSample {
    @Expose
    private float gyroX;
    @Expose
    private float gyroY;
    @Expose
    private float gyroZ;

    public GyroscopeDataSample(float gyroX, float gyroY, float gyroZ) {
        super();
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
    }

    @Override
    public int getDataType() {
        return DATA_SAMPLE_TYPE_GYROSCOPE;
    }

    public float getGyroX() {
        return gyroX;
    }

    public void setGyroX(float gyroX) {
        this.gyroX = gyroX;
    }

    public float getGyroY() {
        return gyroY;
    }

    public void setGyroY(float gyroY) {
        this.gyroY = gyroY;
    }

    public float getGyroZ() {
        return gyroZ;
    }

    public void setGyroZ(float gyroZ) {
        this.gyroZ = gyroZ;
    }
}
