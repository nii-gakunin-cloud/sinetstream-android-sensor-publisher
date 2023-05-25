/*
 * Copyright (c) 2022 National Institute of Informatics
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

public class RemoteKeyItem {
    private int mId;
    private String mFingerprint;
    private String mComment;
    private boolean mIsDefault;
    private String mCreatedAt;

    public RemoteKeyItem(int id,
                         String fingerprint,
                         String comment,
                         boolean isDefault,
                         String createdAt) {
        this.mId = id;
        this.mFingerprint = fingerprint;
        this.mComment = comment;
        this.mIsDefault = isDefault;
        this.mCreatedAt = createdAt;
    }

    public void setId(int id) {
        mId = id;
    }

    public final int getId() {
        return mId;
    }

    public void setFingerprint(@NonNull String fingerprint) {
        mFingerprint = fingerprint;
    }

    public final String getFingerprint() {
        return mFingerprint;
    }

    public void setComment(@NonNull String comment) {
        mComment = comment;
    }

    public final String getComment() {
        return mComment;
    }

    public void setIsDefault(boolean isDefault) {
        mIsDefault = isDefault;
    }

    public boolean getIsDefault() {
        return mIsDefault;
    }

    public void setCreatedAt(@NonNull String createdAt) {
        mCreatedAt = createdAt;
    }

    public final String getCreatedAt() {
        return mCreatedAt;
    }

    @NonNull
    @Override
    public String toString() {
        String keyItem = "RemoteKeyItem {" + "\n";
        keyItem += "id(" + mId + ")" + ",\n";
        keyItem += "fingerprint(" + mFingerprint + ")" + ",\n";
        keyItem += "comment(" + mComment + ")" + ",\n";
        keyItem += "default(" + mIsDefault + ")" + ",\n";
        keyItem += "createdAt(" + mCreatedAt + ")" + "\n";
        keyItem += "}";
        return keyItem;
    }
}
