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
import com.example.samplepublisher.util.DateTimeUtil;

import java.util.Date;

public class SharedPrefsAccessKey {
    private final SharedPreferences mSharedPref;

    private final String ACCESS_TOKEN_KEY_ADDRESS = "address";
    private final String ACCESS_TOKEN_KEY_USER = "user";
    private final String ACCESS_TOKEN_KEY_SECRET_KEY = "secret-key";
    private final String ACCESS_TOKEN_KEY_EXPIRATION_DATE = "expiration-date";

    public SharedPrefsAccessKey(@NonNull Context context) {
        mSharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_access_token),
                Context.MODE_PRIVATE);
    }

    public void writeAccessToken(
            @NonNull String serverUrl,
            @NonNull String account,
            @NonNull String secretKey,
            @NonNull Date expirationDate) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.clear();

        editor.putString(ACCESS_TOKEN_KEY_ADDRESS, serverUrl);
        editor.putString(ACCESS_TOKEN_KEY_USER, account);
        editor.putString(ACCESS_TOKEN_KEY_SECRET_KEY, secretKey);
        editor.putLong(ACCESS_TOKEN_KEY_EXPIRATION_DATE, expirationDate.getTime());

        editor.apply();
    }

    @Nullable
    public String readAccessTokenServerUrl() {
        return mSharedPref.getString(ACCESS_TOKEN_KEY_ADDRESS, null);
    }

    @Nullable
    public String readAccessTokenAccount() {
        return mSharedPref.getString(ACCESS_TOKEN_KEY_USER, null);
    }

    @Nullable
    public String readAccessTokenSecretKey() {
        return mSharedPref.getString(ACCESS_TOKEN_KEY_SECRET_KEY, null);
    }

    @Nullable
    public String readAccessTokenExpirationDate() {
        long unixTime = mSharedPref.getLong(
                ACCESS_TOKEN_KEY_EXPIRATION_DATE, -1L);
        if (unixTime >= 0L) {
            DateTimeUtil dateTimeUtil = new DateTimeUtil();
            return dateTimeUtil.toIso8601String(unixTime);
        }
        return null;
    }

    public boolean isAccessTokenEmpty() {
        return (readAccessTokenServerUrl() == null ||
                readAccessTokenAccount() == null ||
                readAccessTokenSecretKey() == null ||
                readAccessTokenExpirationDate() == null);
    }

    public boolean isAccessTokenExpired() {
        long unixTime = mSharedPref.getLong(
                ACCESS_TOKEN_KEY_EXPIRATION_DATE, -1L);
        if (unixTime >= 0L) {
            Date currentDate = new Date(System.currentTimeMillis());
            Date expirationDate = new Date(unixTime);
            return !currentDate.before(expirationDate);
        } else {
            /* Preference item has not yet set. Treat as expired */
            return true;
        }
    }
}
