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

public class MqttInFlightSettingsFragment extends PreferenceFragmentCompat {
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
        setPreferencesFromResource(R.xml.settings_mqtt_inflight, rootKey);

        EditTextPreference etp;
        etp = getPreferenceManager().
                findPreference(getString(R.string.pref_key_mqtt_inflight_inflight));
        if (etp != null) {
            etp.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(
                            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                }
            });

            etp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    AppCompatActivity activity = (AppCompatActivity) getActivity();
                    String strval = (String) newValue;
                    int inflight;

                    if (strval.isEmpty()) {
                        return true; /* Reflect changes from non-empty to empty */
                    }
                    try {
                        inflight = Integer.parseInt(strval);
                    } catch (NumberFormatException e) {
                        DialogUtil.showErrorDialog(activity,
                                "InFlight: " + e,
                                null, false);
                        return false;
                    }

                    if (inflight < 0) {
                        DialogUtil.showErrorDialog(activity,
                                "InFlight(" + inflight + ") out of range",
                                null, false);
                        return false;
                    }
                    return true;
                }
            });
        }
    }
}
