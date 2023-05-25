/*
 * Copyright (c) 2021 National Institute of Informatics
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

package com.example.samplepublisher.ui.settings;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.example.samplepublisher.R;
import com.example.samplepublisher.util.DialogUtil;

public class SensorSettingsFragment extends PreferenceFragmentCompat {
    /* Markers to check if both longitude and latitude has set, or both unset */
    private boolean mValidLongitude = false;
    private boolean mValidLatitude = false;

    /**
     * Called during {@link #onCreate(Bundle)} to supply the preferences for this fragment.
     * Subclasses are expected to call {@link #setPreferenceScreen(PreferenceScreen)} either
     * directly or via helper methods such as {@link #addPreferencesFromResource(int)}.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     *                           this is the state.
     * @param rootKey            If non-null, this preference fragment should be rooted at the
     *                           {@link PreferenceScreen} with this key.
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_sensors, rootKey);
        EditTextPreference etp;

        etp = findPreference(getString(R.string.pref_key_sensor_interval_timer));
        if (etp != null) {
            etp.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            /* Limit input type to decimal numbers */
                            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                        }
                    }
            );

            etp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    String strval = (String) newValue;
                    long duration;

                    if (strval.isEmpty()) {
                        return true; /* Reflect changes from non-empty to empty */
                    }
                    try {
                        duration = Long.parseLong(strval);
                    } catch (NumberFormatException e) {
                        AppCompatActivity activity = (AppCompatActivity) getActivity();
                        DialogUtil.showErrorDialog(activity,
                                "IntervalTimer: " + e,
                                null, false);
                        return false;
                    }

                    /*
                     * SensorEvent.timestamp is set in nanoseconds.
                     * To prevent overload, we handle interval timer with appropriate lower bound.
                     */
                    if (duration < 1L) {
                        AppCompatActivity activity = (AppCompatActivity) getActivity();
                        DialogUtil.showErrorDialog(activity,
                                "IntervalTimer(" + duration + ") too short",
                                null, false);
                        return false;
                    }
                    return true;
                }
            });
        }

        etp = findPreference(getString(R.string.pref_key_location_longitude));
        if (etp != null) {
            etp.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            /* Limit input type to SIGNED decimal numbers and a point */
                            editText.setInputType(
                                    InputType.TYPE_CLASS_NUMBER |
                                            InputType.TYPE_NUMBER_FLAG_SIGNED |
                                            InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        }
                    }
            );

            etp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    AppCompatActivity activity = (AppCompatActivity) getActivity();
                    String strval = (String) newValue;
                    double longitude;

                    if (strval.isEmpty()) {
                        return true; /* Reflect changes from non-empty to empty */
                    }
                    try {
                        longitude = Double.parseDouble(strval);
                    } catch (NumberFormatException e) {
                        DialogUtil.showErrorDialog(activity,
                                "Longitude: " + e,
                                null, false);
                        return false;
                    }

                    if ((longitude < -180.0 || 180.0 < longitude)) {
                        DialogUtil.showErrorDialog(activity,
                                "Longitude(" + longitude + ") out of range",
                                null, false);
                        return false;
                    }
                    mValidLongitude = true;
                    return true;
                }
            });
        }

        etp = findPreference(getString(R.string.pref_key_location_latitude));
        if (etp != null) {
            etp.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            /* Limit input type to SIGNED decimal numbers and a point */
                            editText.setInputType(
                                    InputType.TYPE_CLASS_NUMBER |
                                            InputType.TYPE_NUMBER_FLAG_SIGNED |
                                            InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        }
                    }
            );

            etp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    AppCompatActivity activity = (AppCompatActivity) getActivity();
                    String strval = (String) newValue;
                    double latitude;

                    if (strval.isEmpty()) {
                        return true; /* Reflect changes from non-empty to empty */
                    }
                    try {
                        latitude = Double.parseDouble(strval);
                    } catch (NumberFormatException e) {
                        DialogUtil.showErrorDialog(activity,
                                "Latitude: " + e,
                                null, false);
                        return false;
                    }

                    if (latitude < -90.0 || 90.0 < latitude) {
                        DialogUtil.showErrorDialog(activity,
                                "Latitude(" + latitude + ") out of range",
                                null, false);
                        return false;
                    }
                    mValidLatitude = true;
                    return true;
                }
            });
        }
    }

    @Override
    public void onStop() {
        if ((mValidLongitude && !mValidLatitude)
                || (!mValidLongitude && mValidLatitude)) {
            DialogUtil.showErrorDialog((AppCompatActivity) getActivity(),
                    "Incomplete location {longitude, latitude} pair",
                    null, false);
        }
        super.onStop();
    }
}
