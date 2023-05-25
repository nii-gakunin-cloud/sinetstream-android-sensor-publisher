/*
 * Copyright (c) 2023 National Institute of Informatics
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

package com.example.samplepublisher.ui.configserver;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.samplepublisher.R;

public class SharedPrefsConfigServer {
    private final SharedPreferences mSharedPref;

    private final String CONFIG_PARAM_DATA_STREAM = "data-stream";
    private final String CONFIG_PARAM_SERVICE_NAME = "service-name";

    public SharedPrefsConfigServer(@NonNull Context context) {
        mSharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_config_server),
                Context.MODE_PRIVATE);
    }

    public void writeConfigServerPrefs(
            @NonNull String name, @NonNull String service) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.clear();

        editor.putString(CONFIG_PARAM_DATA_STREAM, name);
        editor.putString(CONFIG_PARAM_SERVICE_NAME, service);

        editor.apply();
    }

    @Nullable
    public String getDataStream() {
        return mSharedPref.getString(CONFIG_PARAM_DATA_STREAM, null);
    }

    @Nullable
    public String getServiceName() {
        return mSharedPref.getString(CONFIG_PARAM_SERVICE_NAME, null);
    }

    public boolean isAllSet() {
        return (getDataStream() != null && getServiceName() != null);
    }
}
