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
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.example.samplepublisher.R;

public class ConfigServerSettingsFragment extends PreferenceFragmentCompat {
    private final String TAG = ConfigServerSettingsFragment.class.getSimpleName();

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
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings_config_server, rootKey);
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();
        readAccessToken();
    }

    private void readAccessToken() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "Context not found");
            return;
        }

        /* Manipulate item dependency by the ConfigServer settings */
        SharedPrefsAccessKey sharedPrefsAccessKey = new SharedPrefsAccessKey(context);
        boolean enabled =
                (!sharedPrefsAccessKey.isAccessTokenEmpty() &&
                 !sharedPrefsAccessKey.isAccessTokenExpired());

        SharedPrefsConfigServer sharedPrefsConfigServer = new SharedPrefsConfigServer(context);
        boolean canDryRun = (sharedPrefsConfigServer.getDataStream() != null);

        togglePreferences(enabled, canDryRun);
    }

    private void togglePreferences(boolean enabled, boolean canDryRun) {
        Preference preference;
        preference = findPreference(getString(R.string.pref_key_config_server_keypair));
        if (preference != null) {
            preference.setEnabled(enabled);
        }
        preference = findPreference(getString(R.string.pref_key_config_server_select_config));
        if (preference != null) {
            preference.setEnabled(enabled);
        }
        preference = findPreference(getString(R.string.pref_key_config_server_show_config));
        if (preference != null) {
            preference.setEnabled(enabled && canDryRun);
        }
    }
}
