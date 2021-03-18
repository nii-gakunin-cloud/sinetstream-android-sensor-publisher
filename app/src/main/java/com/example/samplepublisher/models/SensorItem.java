package com.example.samplepublisher.models;

import androidx.annotation.NonNull;

public class SensorItem {
    private final int mSensorType;
    private final String mSensorName;

    public SensorItem(int sensorType, String sensorName) {
        this.mSensorType = sensorType;
        this.mSensorName = sensorName;
    }

    public final int getSensorType() {
        return mSensorType;
    }

    public final String getSensorName() {
        return mSensorName;
    }

    @NonNull
    @Override
    public String toString() {
        return "SensorItem{" +
                "mSensorType=" + mSensorType +
                ", mSensorName='" + mSensorName + '\'' +
                '}';
    }
}
