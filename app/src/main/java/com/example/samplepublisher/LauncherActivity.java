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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.samplepublisher.constants.BundleKeys;
import com.example.samplepublisher.util.DialogUtil;

public class LauncherActivity extends AppCompatActivity {
    private final String TAG = LauncherActivity.class.getSimpleName();

    private final int ACTIVITY_CODE_MAIN = 1;
    private final int ACTIVITY_CODE_SETTINGS = 2;

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
                    startActivityForResult(intent, ACTIVITY_CODE_MAIN);
                }
            });
        }

        ImageButton imageButtonSettings = findViewById(R.id.button_settings);
        if (imageButtonSettings != null) {
            imageButtonSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), SettingsActivity.class);
                    startActivityForResult(intent, ACTIVITY_CODE_SETTINGS);
                }
            });
        }

        mSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        /*
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.activity_name_launcher);
        }
         */
    }

    @Override
    protected void onStart() {
        super.onStart();
        toggleRunButton();
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
    public boolean onCreateOptionsMenu(Menu menu) {
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
        final String descriptions =
                "<p>Version: " + BuildConfig.VERSION_NAME +
                        " (" + BuildConfig.BUILD_TYPE + ")" + "</p>" +
                "<br>" +
                "<p>See " +
                "<a href=\"https://www.sinetstream.net/\">SINETStream</a> " +
                "for details.</p>";

        DialogUtil.showSimpleDialog(this, descriptions, true);
    }

    /**
     * Dispatch incoming result to the correct fragment.
     *
     * @param requestCode    User specified code at startActivityForResult
     * @param resultCode    Return value set by the called Activity.
     * @param data    Called Activity might set extra info
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult: " +
                "requestCode(" + requestCode + "),resultCode(" + resultCode + ")");
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ACTIVITY_CODE_SETTINGS:
                toggleRunButton();
                break;
            case ACTIVITY_CODE_MAIN:
            default:
                break;
        }
    }

    private void toggleRunButton() {
        ImageButton imageButtonRun = findViewById(R.id.button_run);
        if (imageButtonRun != null) {
            imageButtonRun.setEnabled(false);
            showMessage("");
        }

        String serviceName = getServiceName();
        if (serviceName == null || serviceName.isEmpty()) {
            showMessage("Please set ServiceName");
            return;
        }

        String topicNames = getTopicNames();
        if (topicNames == null || topicNames.isEmpty()) {
            showMessage("Please set TopicNames");
            return;
        }

        String brokerAddress = getBrokerAddress();
        if (brokerAddress == null || brokerAddress.isEmpty()) {
            showMessage("Please set Broker address");
            return;
        }

        String port = getBrokerListenPort();
        if (port == null || port.isEmpty()) {
            showMessage("Please set Broker listen port");
            return;
        }

        if (!verifyLocation()) {
            showMessage("Please set location {lon, lat} pair");
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

    private boolean verifyLocation() {
        String key1 = getString(R.string.pref_key_location_longitude);
        String key2 = getString(R.string.pref_key_location_latitude);
        String strval1, strval2;
        strval1 = mSharedPreferences.getString(key1, null);
        strval2 = mSharedPreferences.getString(key2, null);

        if ((strval1 != null && !strval1.isEmpty())
                && (strval2 != null && !strval2.isEmpty())) {
            /* Both longitude and latitude is set, OK */
            return true;
        } else if ((strval1 == null || strval1.isEmpty())
                && (strval2 == null || strval2.isEmpty())) {
            /* Both longitude and latitude is NOT set, OK */
            return true;
        }
        return false;
    }
}
