package com.example.samplepublisher.models;

import androidx.annotation.NonNull;

public class SensorItem {
    private final int mSensorType;
    private final String mSensorName;
    private boolean mIsChecked;

    public SensorItem(int sensorType, String sensorName) {
        this.mSensorType = sensorType;
        this.mSensorName = sensorName;
        this.mIsChecked = false;
    }

    public final int getSensorType() {
        return mSensorType;
    }

    public final String getSensorName() {
        return mSensorName;
    }

    public void setChecked(boolean checked) {
        mIsChecked = checked;
    }

    public boolean getChecked() {
        return mIsChecked;
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
