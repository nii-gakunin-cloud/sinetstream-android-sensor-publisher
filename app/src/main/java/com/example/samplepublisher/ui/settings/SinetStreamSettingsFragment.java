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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.example.samplepublisher.R;

public class SinetStreamSettingsFragment extends PreferenceFragmentCompat {
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
    public void onCreatePreferences(
            @Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings_sinetstream, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SwitchPreferenceCompat spc =
                findPreference(getString(R.string.pref_key_toggle_sinetstream_manual_config));
        Preference prefConfigServer =
                findPreference(getString(R.string.pref_category_config_server));
        Preference prefSinetStream =
                findPreference(getString(R.string.pref_category_sinetstream));

        if (spc != null && prefConfigServer != null && prefSinetStream != null) {
            boolean isChecked1 = spc.isChecked();
            prefConfigServer.setEnabled(!isChecked1);
            prefSinetStream.setEnabled(isChecked1);

            spc.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        /**
                         * Called when a preference has been changed by the user. This is called before the state
                         * of the preference is about to be updated and before the state is persisted.
                         *
                         * @param preference The changed preference
                         * @param newValue   The new value of the preference
                         * @return {@code true} to update the state of the preference with the new value
                         */
                        @Override
                        public boolean onPreferenceChange(
                                @NonNull Preference preference, Object newValue) {
                            boolean isChecked2 = (Boolean) newValue;
                            prefConfigServer.setEnabled(!isChecked2);
                            prefSinetStream.setEnabled(isChecked2);
                            return true;
                        }
                    }
            );
        }
    }
}
