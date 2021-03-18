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
import android.text.TextUtils;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.example.samplepublisher.R;

public class TlsSettingsFragment extends PreferenceFragmentCompat {
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
        setPreferencesFromResource(R.xml.settings_tls, rootKey);

        EditTextPreference etp;
        etp = getPreferenceManager().
                findPreference(getString(R.string.pref_key_tls_ca_certs));
        if (etp != null) {
            etp.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(
                            InputType.TYPE_CLASS_TEXT);
                }
            });
        }

        etp = getPreferenceManager().
                findPreference(getString(R.string.pref_key_tls_certfile));
        if (etp != null) {
            etp.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(
                            InputType.TYPE_CLASS_TEXT);
                }
            });
        }

        etp = getPreferenceManager().
                findPreference(getString(R.string.pref_key_tls_keyfilePassword));
        if (etp != null) {
            etp.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            });

            /*
             * We don't want to expose raw password string to UI components.
             * Instead, use custom summary provider.
             * https://developer.android.com/guide/topics/ui/settings/customize-your-settings#use_a_custom_summaryprovider
             */
            etp.setSummaryProvider(new Preference.SummaryProvider<EditTextPreference>() {
                /**
                 * Called whenever {@link Preference#getSummary()} is called on this preference.
                 *
                 * @param preference This preference
                 * @return A CharSequence that will be displayed as the summary for this preference
                 */
                @Override
                public CharSequence provideSummary(EditTextPreference preference) {
                    String text = preference.getText();
                    if (TextUtils.isEmpty(text)){
                        return getString(R.string.pref_summary_password_notset);
                    }
                    return getString(R.string.pref_summary_password_mask);
                }
            });
        }
    }
}
