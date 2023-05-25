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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.example.samplepublisher.R;

public class AccessTokenFragment extends PreferenceFragmentCompat {
    private final String TAG = AccessTokenFragment.class.getSimpleName();

    private AccessTokenListener mListener = null;

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     *
     * @param context The {@link Context} which implements
     *                the {@link AccessTokenListener}.
     * @throws RuntimeException if the context argument is not
     *                          an instance of {@link AccessTokenListener}.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AccessTokenListener) {
            mListener = (AccessTokenListener) context;
        } else {
            throw new RuntimeException(context +
                    " must implement AccessTokenListener");
        }
    }

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
        setPreferencesFromResource(R.xml.settings_access_token, rootKey);
        readAccessToken();
        setActionsForPickup();
        setActionsForDownload();
    }

    public void readAccessToken() {
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "Context not found");
            return;
        }
        SharedPrefsAccessKey sharedPrefsAccessKey = new SharedPrefsAccessKey(context);

        EditTextPreference etp;
        etp = getPreferenceManager().
                findPreference(getString(R.string.pref_key_access_token_server_url));
        if (etp != null) {
            String s = sharedPrefsAccessKey.readAccessTokenServerUrl();
            if (s != null) {
                etp.setText(s);
                etp.setIcon(R.drawable.check);
            } else {
                etp.setIcon(null);
            }
        }

        etp = getPreferenceManager().
                findPreference(getString(R.string.pref_key_access_token_account));
        if (etp != null) {
            String s = sharedPrefsAccessKey.readAccessTokenAccount();
            if (s != null) {
                etp.setText(s);
                etp.setIcon(R.drawable.check);
            } else {
                etp.setIcon(null);
            }
        }

        etp = getPreferenceManager().
                findPreference(getString(R.string.pref_key_access_token_secret_key));
        if (etp != null) {
            String s = sharedPrefsAccessKey.readAccessTokenSecretKey();
            if (s != null) {
                /* Mask sensitive data */
                s = getString(R.string.pref_summary_password_mask);
                etp.setIcon(R.drawable.check);
            } else {
                etp.setIcon(null);
            }
            etp.setText(s);
        }

        etp = getPreferenceManager().
                findPreference(getString(R.string.pref_key_access_token_expiration_date));
        if (etp != null) {
            String s = sharedPrefsAccessKey.readAccessTokenExpirationDate();
            if (s != null) {
                etp.setText(s);
                if (sharedPrefsAccessKey.isAccessTokenExpired()) {
                    etp.setIcon(R.drawable.alert);
                } else {
                    etp.setIcon(R.drawable.check);
                }
            } else {
                etp.setIcon(null);
            }
        }
    }

    private void setActionsForPickup() {
        Preference preference = getPreferenceManager().
                findPreference(getString(R.string.pref_key_access_token_action_pickup));
        if (preference != null) {
            preference.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        /**
                         * Called when a preference has been clicked.
                         *
                         * @param preference The preference that was clicked
                         * @return {@code true} if the click was handled
                         */
                        @Override
                        public boolean onPreferenceClick(@NonNull Preference preference) {
                            if (mListener != null) {
                                mListener.onCallDocumentPicker();
                                return true;
                            } else {
                                /* Calling sequence failure */
                                Log.w(TAG, "AccessTokenListener has not yet set?");
                                return false;
                            }
                        }
                    });
        }
    }

    private void setActionsForDownload() {
        EditTextPreference etp = getPreferenceManager().
                findPreference(getString(R.string.pref_key_access_token_action_download));
        if (etp != null) {
            etp.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                }
            });
            etp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                        @NonNull Preference preference, Object newValue) {
                    if (newValue instanceof String) {
                        String url = (String) newValue;
                        if (!URLUtil.isValidUrl(url) || !URLUtil.isHttpsUrl(url)) {
                            mListener.onWarning("Sorry, not a valid URL:\n" + url);
                            return false;
                        }

                        Uri webpage = Uri.parse(url);
                        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
                        startActivity(intent);
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    public interface AccessTokenListener {
        void onCallDocumentPicker();
        void onWarning(@NonNull String description);
        void onError(@NonNull String description);
    }
}
