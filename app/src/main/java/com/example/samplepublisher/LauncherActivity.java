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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.example.samplepublisher.constants.ActivityCodes;
import com.example.samplepublisher.constants.BundleKeys;
import com.example.samplepublisher.util.DialogUtil;

public class LauncherActivity extends AppCompatActivity {
    private final String TAG = LauncherActivity.class.getSimpleName();

    private ActivityResultLauncher<Intent> mActivityResultLauncher;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        ImageButton imageButtonRun = findViewById(R.id.button_run);
        if (imageButtonRun != null) {
            imageButtonRun.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), MainActivity.class);
                    String serviceName = getServiceName();
                    if (serviceName != null) {
                        intent.putExtra(BundleKeys.BUNDLE_KEY_SERVICE_NAME, serviceName);
                    }
                    if (useConfigServer()) {
                        intent.putExtra(BundleKeys.BUNDLE_KEY_USE_CONFIG_SERVER, true);
                    }
                    if (isProtocolDebug()) {
                        intent.putExtra(BundleKeys.BUNDLE_KEY_PROTOCOL_DEBUG, true);
                    }
                    if (isCellularDebug()) {
                        intent.putExtra(BundleKeys.BUNDLE_KEY_CELLULAR_DEBUG, true);
                    }
                    if (isLocationDebug()) {
                        intent.putExtra(BundleKeys.BUNDLE_KEY_LOCATION_DEBUG, true);
                    }
                    mActivityResultLauncher.launch(intent);
                }
            });
        }

        ImageButton imageButtonSettings = findViewById(R.id.button_settings);
        if (imageButtonSettings != null) {
            imageButtonSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), SettingsActivity.class);
                    mActivityResultLauncher.launch(intent);
                }
            });
        }

        mSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        /* Use custom Toolbar for flexible setup */
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        /*
         * https://developer.android.com/training/basics/intents/result#register
         */
        mActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Log.d(TAG, "ActivityResultLauncher.onActivityResult: result=" + result);
                        processActivityResult(result);
                    }
                }
        );
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (useConfigServer()) {
            enableRunButton();
        } else {
            toggleRunButton();
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.
     *
     * <p>This is only called once, the first time the options menu is
     * displayed.  To update the menu every time it is displayed, see
     * {@link #onPrepareOptionsMenu}.
     *
     * <p>The default implementation populates the menu with standard system
     * menu items.  These are placed in the {@link Menu#CATEGORY_SYSTEM} group so that
     * they will be correctly ordered with application-defined menu items.
     * Deriving classes should always call through to the base implementation.
     *
     * <p>You can safely hold on to <var>menu</var> (and any items created
     * from it), making modifications to it as desired, until the next
     * time onCreateOptionsMenu() is called.
     *
     * <p>When you add items to the menu, you can implement the Activity's
     * {@link #onOptionsItemSelected} method to handle them there.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_items, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     *
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.</p>
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_help) {
            handleActionHelp();
        } else if (itemId == R.id.action_about) {
            handleActionAbout();
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleActionHelp() {
        final String descriptions = "Under construction...";
        DialogUtil.showSimpleDialog(this, descriptions, false);
    }

    private void handleActionAbout() {
        /*
         * Starting from Android Gradle Plugin 4.1.0, Version properties
         * (VERSION_NAME and BUILD_TYPE) have removed from BuildConfig class
         * in library projects.
         * Instead, we extract version properties from PackageInfo.
         *
         * https://developer.android.com/studio/releases/past-releases#4-1-0
         */
        PackageManager pm = this.getPackageManager();
        PackageInfo packageInfo;
        try {
            packageInfo = pm.getPackageInfo(this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }

        String versionName = packageInfo.versionName;
        String versionCode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            versionCode = String.valueOf(packageInfo.getLongVersionCode());
        } else {
            versionCode = String.valueOf(packageInfo.versionCode);
        }

        String descriptions = "";
        descriptions += "<p>Version: " + versionName +
                " (" + versionCode + ")" + "</p>";
        descriptions += "<br>";
        descriptions += "<p>See " +
                "<a href=\"https://www.sinetstream.net/\">SINETStream</a> " +
                "for details.</p>";
        descriptions += "<br>";
        descriptions += "<p>" +
                "<a href=\"https://github.com/nii-gakunin-cloud/sinetstream-android-sensor-publisher/blob/main/licenses/README.md\">Licensing Information</a>" +
                "</p>";

        DialogUtil.showSimpleDialog(this, descriptions, true);
    }

    private void processActivityResult(@NonNull ActivityResult result) {
        Log.d(TAG, "processActivityResult: " + result);
        final int resultCode = result.getResultCode();
        switch (resultCode) {
            case Activity.RESULT_OK:
                Intent intent = result.getData();
                if (intent != null) {
                    int code = intent.getIntExtra(
                            ActivityCodes.KEY, ActivityCodes.ACTIVITY_CODE_UNSPECIFIED);
                    switch (code) {
                        case ActivityCodes.ACTIVITY_CODE_MAIN:
                            Log.d(TAG, "MainActivity has finished");
                            break;
                        case ActivityCodes.ACTIVITY_CODE_SETTINGS:
                            Log.d(TAG, "SettingsActivity has finished");
                            break;
                        default:
                            break;
                    }
                }
                break;
            case Activity.RESULT_CANCELED:
                Log.d(TAG, "Back from SettingsActivity");
                break;
            default:
                break;
        }
    }

    private void enableRunButton() {
        TextView tv = findViewById(R.id.button_label_run);
        if (tv != null) {
            tv.setText(R.string.button_run_with_config_server);
        }
        ImageButton imageButtonRun = findViewById(R.id.button_run);
        if (imageButtonRun != null) {
            imageButtonRun.setEnabled(true);
            showMessage("");
        }
    }

    private void toggleRunButton() {
        TextView tv = findViewById(R.id.button_label_run);
        if (tv != null) {
            tv.setText(R.string.button_run);
        }
        ImageButton imageButtonRun = findViewById(R.id.button_run);
        if (imageButtonRun != null) {
            imageButtonRun.setEnabled(false);
            showMessage("");
        }

        String serviceName = getServiceName();
        if (serviceName == null || serviceName.isEmpty()) {
            showMessage(getString(R.string.incomplete_service_settings));
            return;
        }

        String topicNames = getTopicNames();
        if (topicNames == null || topicNames.isEmpty()) {
            showMessage(getString(R.string.incomplete_service_settings));
            return;
        }

        String brokerAddress = getBrokerAddress();
        if (brokerAddress == null || brokerAddress.isEmpty()) {
            showMessage(getString(R.string.incomplete_broker_settings));
            return;
        }

        String port = getBrokerListenPort();
        if (port == null || port.isEmpty()) {
            showMessage(getString(R.string.incomplete_broker_settings));
            return;
        }

        if (imageButtonRun != null) {
            imageButtonRun.setEnabled(true);
            showMessage("");
        }
    }

    private void showMessage(@Nullable String message) {
        TextView tv = findViewById(R.id.settings_status);
        if (tv != null && message != null) {
            tv.setText(message);
        }
    }

    private boolean useConfigServer() {
        /* Not manual configuration = use configuration server */
        return (! getPrefsToggleSinetstreamManualConfig());
    }

    private boolean getPrefsToggleSinetstreamManualConfig() {
        String key = getString(R.string.pref_key_toggle_sinetstream_manual_config);
        return mSharedPreferences.getBoolean(key, true);
    }

    private boolean isProtocolDebug() {
        String key = getString(R.string.pref_key_toggle_protocol_debug);
        return mSharedPreferences.getBoolean(key, false);
    }

    private boolean isCellularDebug() {
        String key = getString(R.string.pref_key_toggle_cellular_debug);
        return mSharedPreferences.getBoolean(key, false);
    }

    private boolean isLocationDebug() {
        String key = getString(R.string.pref_key_toggle_location_debug);
        return mSharedPreferences.getBoolean(key, false);
    }

    @Nullable
    private String getServiceName() {
        String key = getString(R.string.pref_key_service_name);
        return mSharedPreferences.getString(key, null);
    }

    @Nullable
    private String getTopicNames() {
        String key = getString(R.string.pref_key_service_topics);
        return mSharedPreferences.getString(key, null);
    }

    @Nullable
    private String getBrokerAddress() {
        String key = getString(R.string.pref_key_broker_network_address);
        return mSharedPreferences.getString(key, null);
    }

    @Nullable
    private String getBrokerListenPort() {
        String key = getString(R.string.pref_key_broker_listen_port);
        return mSharedPreferences.getString(key, null);
    }
}
