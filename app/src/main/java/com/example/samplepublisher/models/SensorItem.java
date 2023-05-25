/*
 * Copyright (c) 2020 National Institute of Informatics
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
