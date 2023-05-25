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

package com.example.samplepublisher;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.samplepublisher.constants.BundleKeys;
import com.example.samplepublisher.ui.configserver.AccessTokenFragment;
import com.example.samplepublisher.ui.configserver.ConfigCheckerFragment;
import com.example.samplepublisher.ui.configserver.ConfigServerSettingsFragment;
import com.example.samplepublisher.ui.configserver.SharedPrefsAccessKey;
import com.example.samplepublisher.ui.dialogs.ErrorDialogFragment;
import com.example.samplepublisher.ui.settings.RootSettingsFragment;
import com.example.samplepublisher.ui.settings.Xml2Yaml;
import com.google.android.material.snackbar.Snackbar;

import java.util.Date;

import jp.ad.sinet.stream.android.config.remote.ConfigServerSettings;

public class SettingsActivity extends AppCompatActivity implements
        AccessTokenFragment.AccessTokenListener,
        ConfigCheckerFragment.ConfigCheckerListener,
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        SharedPreferences.OnSharedPreferenceChangeListener,
        ErrorDialogFragment.ErrorDialogListener {
    private final String TAG = SettingsActivity.class.getSimpleName();

    private AccessTokenFragment mAccessTokenFragment = null;
    private ConfigServerSettings mConfigServerSettings = null;
    private boolean mPreferenceChanged = false;

    private SharedPrefsAccessKey mSharedPrefsAccessKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState != null) {
            /* Avoid creating the same fragment sets more than once. */
            Log.d(TAG, "onCreate: After RESTART");
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new RootSettingsFragment())
                    .commit();

            /* LifecycleOwners must call register before they are STARTED */
            setupRemoteConfiguration();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.activity_name_settings);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true); // onOptionsItemSelected
        }

        mSharedPrefsAccessKey = new SharedPrefsAccessKey(this);
    }

    @Override
    protected void onDestroy() {
        clearRemoteConfiguration();
        super.onDestroy();
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.
     */
    @Override
    protected void onResume() {
        super.onResume();

        /* Start catching SharedPreferenceChanged event */
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();

        /* Stop catching SharedPreferenceChanged event */
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        if (mPreferenceChanged) {
            Xml2Yaml xml2Yaml = new Xml2Yaml(this);
            xml2Yaml.setupInfo();
        }
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        /* Back to the parent activity, as specified in the app manifest */
        if (item.getItemId() == android.R.id.home) {
            //showConfirmDialog();
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the user has clicked on a preference that has a fragment class name
     * associated with it. The implementation should instantiate and switch to an instance
     * of the given fragment.
     *
     * @param caller The fragment requesting navigation
     * @param pref   The preference requesting the fragment
     * @return {@code true} if the fragment creation has been handled
     */
    @Override
    public boolean onPreferenceStartFragment(
            @NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        /* Implementation of PreferenceFragmentCompat.OnPreferenceStartFragmentCallback */
        // Instantiate the new Fragment
        String className = pref.getFragment();
        if (className == null) {
            className = "N/A";
        }
        final Bundle args = pref.getExtras();
        final Fragment fragment =
                getSupportFragmentManager().
                        getFragmentFactory().instantiate(
                                getClassLoader(), className);

        if (fragment instanceof AccessTokenFragment) {
            /* Keep the fragment for later reference */
            mAccessTokenFragment = (AccessTokenFragment) fragment;
        }
        if (fragment instanceof ConfigServerSettingsFragment ||
                fragment instanceof AccessTokenFragment) {
            if (mSharedPrefsAccessKey.isAccessTokenEmpty()) {
                showSnackBar(getString(R.string.snackbar_access_token_empty));
            } else if (mSharedPrefsAccessKey.isAccessTokenExpired()) {
                showSnackBar(getString(R.string.snackbar_access_token_expired));
            }
        }
        fragment.setArguments(args);

        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().
                beginTransaction().
                replace(R.id.settings_container, fragment).
                addToBackStack(null).
                commit();
        return true;
    }

    /**
     * Called when a shared preference is changed, added, or removed. This
     * may be called even if a preference is set to its existing value.
     *
     * <p>This callback will be run on your main thread.
     *
     * <p><em>Note: This callback will not be triggered when preferences are cleared
     * via {@link SharedPreferences.Editor#clear()}, unless targeting {@link Build.VERSION_CODES#R}
     * on devices running OS versions {@link Build.VERSION_CODES#R Android R}
     * or later.</em>
     *
     * @param sharedPreferences The {@link SharedPreferences} that received the change.
     * @param key               The key of the preference that was changed, added, or removed. Apps targeting
     *                          {@link Build.VERSION_CODES#R} on devices running OS versions
     *                          {@link Build.VERSION_CODES#R Android R} or later, will receive
     *                          a {@code null} value when preferences are cleared.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        /* Implementation of SharedPreferences.OnSharedPreferenceChangeListener */
        Log.d(TAG, "OnSharedPreferenceChanged: key(" + key + ")");
        mPreferenceChanged = true;
    }

    @Override
    public void onCallDocumentPicker() {
        /* Implementation of AccessTokenFragment.AccessTokenListener */
        if (mConfigServerSettings != null) {
            mConfigServerSettings.launchDocumentPicker();
        }
    }

    private void setupRemoteConfiguration() {
        Log.d(TAG, "setupRemoteConfiguration");
        mConfigServerSettings = new ConfigServerSettings(this,
                new ConfigServerSettings.ConfigServerSettingsListener() {
                    @Override
                    public void onParsed(@NonNull String serverUrl,
                                         @NonNull String account,
                                         @NonNull String secretKey,
                                         @NonNull Date expirationDate) {
                        mSharedPrefsAccessKey.writeAccessToken(
                                serverUrl, account, secretKey, expirationDate
                        );
                        showSnackBar(getString(R.string.snackbar_access_token_load_ok));

                        if (mAccessTokenFragment != null) {
                            mAccessTokenFragment.readAccessToken();
                        }
                    }

                    @Override
                    public void onExpired() {
                        SettingsActivity.this.onWarning(
                                getString(R.string.auth_json_expired));
                    }

                    @Override
                    public void onError(@NonNull String description) {
                        SettingsActivity.this.onError(description);
                    }
                });
    }

    private void clearRemoteConfiguration() {
        Log.d(TAG, "clearRemoteConfiguration");
        if (mConfigServerSettings != null) {
            mConfigServerSettings.clearDocumentPicker();
            mConfigServerSettings = null;
        }
    }

    private void showSnackBar(@NonNull String message) {
        FrameLayout layout = findViewById(R.id.settings_container);
        Snackbar snackbar = Snackbar.make(layout, message, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    @Override
    public void onWarning(@NonNull String description) {
        /* Implementation of AccessTokenFragment.AccessTokenListener */
        showErrorDialog(description, false);
    }

    @Override
    public void onError(@NonNull String description) {
        /* Implementation of AccessTokenFragment.AccessTokenListener */
        /* Implementation of ConfigCheckerFragment.ConfigCheckerListener */
        showErrorDialog(description, true);
    }

    private void showErrorDialog(@NonNull String description, boolean isFatal) {
        ErrorDialogFragment edf = new ErrorDialogFragment(this);
        Bundle bundle = new Bundle();
        bundle.putString(BundleKeys.BUNDLE_KEY_ERROR_MESSAGE, description);
        bundle.putBoolean(BundleKeys.BUNDLE_KEY_ERROR_FATAL, isFatal);
        edf.setArguments(bundle);

        try {
            edf.show(getSupportFragmentManager(), ErrorDialogFragment.class.getSimpleName());
        } catch (IllegalStateException e) {
            Log.e(TAG, "XXX: ErrorDialogFragment: " + e);
            onErrorDialogDismissed(true);
        }

        /*
         * If user pressed OK button on the error dialog window,
         * ErrorDialogFragment.onErrorDialogDismissed() will be called.
         */
    }

    @Override
    public void onErrorDialogDismissed(boolean isFatal) {
        /* Implementation of ErrorDialogFragment.ErrorDialogListener */
        if (isFatal) {
            onBackPressed();
        }
    }
}
