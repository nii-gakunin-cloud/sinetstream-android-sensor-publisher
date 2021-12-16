/*
 * Copyright (c) 2020-2021 National Institute of Informatics
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
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplepublisher.constants.ActivityCodes;
import com.example.samplepublisher.constants.BundleKeys;
import com.example.samplepublisher.ui.main.ErrorDialogFragment;
import com.example.samplepublisher.ui.main.MainFragment;
import com.example.samplepublisher.ui.main.SendFragment;
import com.example.samplepublisher.ui.main.SensorItemAdapter;
import com.example.samplepublisher.ui.main.SensorViewModel;
import com.example.samplepublisher.util.DialogUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import jp.ad.sinet.stream.android.api.SinetStreamWriterString;
import jp.ad.sinet.stream.android.helper.LocationTracker;
import jp.ad.sinet.stream.android.helper.LocationTrackerListener;
import jp.ad.sinet.stream.android.helper.SensorController;
import jp.ad.sinet.stream.android.helper.SensorListener;
import jp.ad.sinet.stream.android.net.cert.KeyChainHandler;

public class MainActivity extends AppCompatActivity implements
        MainFragment.OnFragmentInteractionListener,
        SendFragment.SendFragmentListener,
        SinetStreamWriterString.SinetStreamWriterStringListener,
        ErrorDialogFragment.ErrorDialogListener,
        LocationTrackerListener,
        SensorListener {
    private final String TAG = MainActivity.class.getSimpleName();

    private SensorViewModel mViewModel = null;
    private final int mClientId = 1;
    private SensorController mSensorController = null;
    private ArrayList<Integer> mRunningSensorTypes = null;

    private LocationTracker mLocationTracker = null;
    private boolean mIsLocationTrackerReady = false;
    private boolean mPrefsLocation = false;
    private boolean mPrefsLocationAutoUpdate = false;
    private String mLocationProvider = null;
    private final String LOCATION_PROVIDER_FIXED = "fixed";
    private Location mLocationCache = null;

    private boolean mIsWriterAvailable = false;

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        /*
         * Keep some attributes beyond Activity's lifecycle.
         */
        mViewModel = new ViewModelProvider(this).
                get(SensorViewModel.class);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.activity_name_main);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true); // onOptionsItemSelected
        }

        if (savedInstanceState != null) {
            /* Avoid creating the same fragment sets more than once. */
            Log.d(TAG, "onCreate: After RESTART");
        } else {
            /*
             * In case this activity was started with special instructions from an
             * Intent, pass the Intent's extras to the fragment as arguments
             */
            Bundle bundle = null;
            Intent intent = getIntent();
            if (intent != null) {
                bundle = intent.getExtras();
            }

            /* Check shared preference for location settings */
            checkLocationSettings();

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            MainFragment mainFragment = new MainFragment();
            if (mPrefsLocation) {
                Bundle bundle1 = new Bundle();
                final String provider;
                if (mPrefsLocationAutoUpdate) {
                    startLocationTracker(fragmentManager);
                    provider = mLocationProvider;
                } else {
                    provider = LOCATION_PROVIDER_FIXED;
                }
                bundle1.putString(BundleKeys.BUNDLE_KEY_LOCATION_PROVIDER,
                        provider.toUpperCase(Locale.US));
                mainFragment.setArguments(bundle1);
            }
            transaction.replace(R.id.container, mainFragment, "MainFragment");

            /*
             * Since we don't have to control the SINETStream Writer module
             * via specific UI, we allocate the SendFragment as a UI-less
             * worker module.
             */
            SendFragment sendFragment = new SendFragment();
            sendFragment.setArguments(bundle);
            transaction.add(sendFragment, "SendFragment");

            transaction.commit();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopLocationTracker();
        super.onDestroy();
    }

    private void checkLocationSettings() {
        String key1 = getString(R.string.pref_key_toggle_location);
        String key2 = getString(R.string.pref_key_toggle_location_auto_update);
        String key3 = getString(R.string.pref_key_location_provider);
        if (mSharedPreferences.getBoolean(key1, false)) {
            mPrefsLocation = true;
            if (mSharedPreferences.getBoolean(key2, false)) {
                mPrefsLocationAutoUpdate = true;
                mLocationProvider = mSharedPreferences.getString(
                        key3, getString(R.string.pref_default_location_provider));
                Log.d(TAG, "Location (" + mLocationProvider + "): ENABLED");
            } else {
                Log.d(TAG, "Location (" + LOCATION_PROVIDER_FIXED + "): ENABLED");
            }
        } else {
            Log.d(TAG, "Location: DISABLED");
        }
    }

    private void startLocationTracker(FragmentManager fragmentManager) {
        /*
         * Now that "Activity.onAttachFragment()" has deprecated, we handle the
         * fragment attach event by
         * {@link FragmentManager#addFragmentOnAttachListener(FragmentOnAttachListener)}.
         */
        fragmentManager.addFragmentOnAttachListener(new FragmentOnAttachListener() {
            @Override
            public void onAttachFragment(
                    @NonNull FragmentManager fragmentManager1,
                    @NonNull Fragment fragment) {
                Log.d(TAG, "onAttachFragment");

                if (fragment instanceof MainFragment) {
                    if (mPrefsLocationAutoUpdate) {
                        Log.d(TAG, "Going to start LocationTracker...");
                        boolean useGpsProvider =
                                (mLocationProvider != null
                                        && mLocationProvider.equals(
                                                getString(R.string.pref_default_location_provider)));
                        if (useGpsProvider) {
                            mLocationTracker =
                                    new LocationTracker(
                                            MainActivity.this,
                                            LocationManager.GPS_PROVIDER,
                                            mClientId);
                        } else {
                            mLocationTracker =
                                    new LocationTracker(
                                            MainActivity.this,
                                            LocationManager.FUSED_PROVIDER,
                                            mClientId);
                        }
                        mLocationTracker.start();
                    }
                }
            }
        });
    }

    private void stopLocationTracker() {
        if (mLocationTracker != null) {
            mLocationTracker.stop();
        }
    }

    private boolean getPrefsToggleSslTls() {
        String key = getString(R.string.pref_key_toggle_tls);
        return mSharedPreferences.getBoolean(key, false);
    }

    private boolean getPrefsServerCertificates() {
        String key = getString(R.string.pref_key_tls_server_certs);
        return mSharedPreferences.getBoolean(key, false);
    }

    private boolean getPrefsClientCertificates() {
        String key = getString(R.string.pref_key_tls_client_certs);
        return mSharedPreferences.getBoolean(key, false);
    }

    @Nullable
    private MainFragment lookupMainFragment() {
        MainFragment fragment;
        fragment = (MainFragment) getSupportFragmentManager().
                findFragmentByTag("MainFragment");
        if (fragment == null) {
            Log.e(TAG, "MainFragment not found?");
        }
        return fragment;
    }

    @Nullable
    private SendFragment lookupSendFragment() {
        SendFragment fragment;
        fragment = (SendFragment) getSupportFragmentManager().
                findFragmentByTag("SendFragment");
        if (fragment == null) {
            Log.e(TAG, "SendFragment not found?");
        }
        return fragment;
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();

        if (getPrefsToggleSslTls()) {
            /* Use SSL/TLS */
            if (getPrefsClientCertificates()) {
                /* Use Client Certificate */
                String alias = mViewModel.getPrivateKeyAlias();
                if (alias != null) {
                    Log.d(TAG, "Re-use certificate alias: " + alias);
                    buildFragments(alias);
                } else {
                    /* Let user select the client certificate */
                    KeyChainHandler kch = new KeyChainHandler();
                    kch.checkCertificate(
                            MainActivity.this,
                            new KeyChainHandler.KeyChainListener() {
                                @Override
                                public void onPrivateKeyAlias(
                                        @Nullable String alias) {
                                    Log.d(TAG, "onPrivateKeyAlias: " + alias);
                                    mViewModel.setPrivateKeyAlias(alias);

                                    if (alias != null) {
                                        buildFragments(alias);
                                    } else {
                                        onError("Client certificate has not chosen");
                                    }
                                }
                            });
                }
            } else {
                /* Don't use Client Certificate */
                buildFragments(null);
            }
        } else {
            /* Don't use SSL/TLS */
            buildFragments(null);
        }
    }

    private void buildFragments(@Nullable String alias) {
        Log.d(TAG, "buildFragments: alias(" + alias + ")");

        SendFragment sendFragment = lookupSendFragment();
        if (sendFragment != null) {
            sendFragment.startWriter(alias);
            toggleProgressBar(true);
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");

        SendFragment sendFragment = lookupSendFragment();
        if (sendFragment != null) {
            /* Prevent timing-dependent NullPointerException */
            if (mIsWriterAvailable) {
                sendFragment.stopWriter();
            }
            mIsWriterAvailable = false; /* Prevent race condition */
        }
        super.onStop();
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.
     */
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        /* Bind LocationService to receive location updates */
        if (mLocationTracker != null) {
            if (mIsLocationTrackerReady) {
                Log.d(TAG, "Going to bind LocationService");
                mLocationTracker.bindLocationService();
            } else {
                Log.d(TAG, "Wait until LocationTracker gets ready");
            }
        }

        /*
         * Bind SensorService after connection has established.
         *
        mSensorController = new SensorController(this, mSensorClientId);
        mSensorController.bindSensorService();
         */
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        /* Unbind LocationService to stop receiving location updates */
        if (mLocationTracker != null) {
            Log.d(TAG, "Going to unbind LocationService");
            mLocationTracker.unbindLocationService();
        }

        /*
         * Unbind SensorService after connection has closed.
         *
        if (mSensorController != null) {
            mSensorController.unbindSensorService();
            mSensorController = null;
        }
         */
        super.onPause();
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

    @Override
    public void onSensorTypesChecked(boolean checked) {
        /* Implementation of MainFragment.onSensorTypesChosen */
        Log.d(TAG, "onSensorTypesChecked: " + checked);

        MainFragment mainFragment = lookupMainFragment();
        if (mainFragment == null) {
            return;
        }
        RecyclerView recyclerView = findViewById(R.id.sensorItemList);
        if (recyclerView != null) {
            SensorItemAdapter sensorItemAdapter =
                    (SensorItemAdapter) recyclerView.getAdapter();
            if (sensorItemAdapter != null) {
                mainFragment.enableSensorOnOffButton(checked);
            } else {
                Log.w(TAG, "SensorItemAdapter has gone?");
            }
        } else {
            Log.w(TAG, "RecyclerView has gone?");
        }
    }

    @Override
    public void onEnableSensors() {
        /* Implementation of MainFragment.onEnableSensors */
        Log.d(TAG, "onEnableSensors");

        RecyclerView recyclerView = findViewById(R.id.sensorItemList);
        if (recyclerView != null) {
            SensorItemAdapter sensorItemAdapter =
                    (SensorItemAdapter) recyclerView.getAdapter();
            if (sensorItemAdapter != null) {
                mRunningSensorTypes =
                        sensorItemAdapter.getCheckedSensorTypes();
                /* SPECIAL HANDLING FOR DISPLAY ROTATION */
                /*
                 * If this Activity has gone into background and then comes
                 * back to foreground, the view hierarchy (GUI parts) will be
                 * restored by FragmentStateManager.
                 * Since this process is done during Fragment#onActivityCreated
                 * and Fragment#onStart, NullPointerException will occur at
                 * mSensorController.enableSensors().
                 *
                 * We should wait until onResume() where SensorController object
                 * is created and SensorController#bindService is called.
                 */
                if (mSensorController != null) {
                    mSensorController.enableSensors(mRunningSensorTypes);
                    mViewModel.setSensorRunning(true);
                } else {
                    /* Display rotation case */
                    Log.d(TAG, "SensorController is still OFFLINE");
                }

                /* Prevent touching sensor list while running */
                sensorItemAdapter.enableSensorTypes(false);
            }
        } else {
            Log.w(TAG, "RecyclerView has gone?");
        }
    }

    @Override
    public void onDisableSensors() {
        /* Implementation of MainFragment.onDisableSensors */
        Log.d(TAG, "onDisableSensors");

        RecyclerView recyclerView = findViewById(R.id.sensorItemList);
        if (recyclerView != null) {
            SensorItemAdapter sensorItemAdapter =
                    (SensorItemAdapter) recyclerView.getAdapter();
            if (sensorItemAdapter != null) {
                if (mSensorController != null) {
                    mSensorController.disableSensors(mRunningSensorTypes);
                    mViewModel.setSensorRunning(false);
                } else {
                    /* Display rotation case */
                    Log.d(TAG, "SensorController is still OFFLINE");
                }
                mRunningSensorTypes = null;

                /* Enable sensor list again */
                sensorItemAdapter.enableSensorTypes(true);
            }
        } else {
            Log.w(TAG, "RecyclerView has gone?");
        }
    }

    /**
     * As the successful response of {@link SensorController#getAvailableSensorTypes},
     * SensorService returns the pair of ArrayLists, one for available
     * sensor types, and the other for those sensor type names.
     * <p>
     * You can access each ArrayList elements as follows.
     * <pre>{@code
     *         for (int i = 0; i < sensorTypes.size(i); i++) {
     *             int sensorType = sensorTypes.get(i);
     *             String sensorTypeName = sensorTypeNames.get(i);
     *             ...
     *         }
     *     }</pre>
     * </p>
     *
     * @param sensorTypes     ArrayList of available sensor types
     * @param sensorTypeNames ArrayList of sensor type names, such as "accelerometer"
     */
    @Override
    public void onSensorTypesReceived(
            @NonNull ArrayList<Integer> sensorTypes,
            @NonNull ArrayList<String> sensorTypeNames) {
        Log.d(TAG, "onSensorTypesReceived");

        MainFragment mainFragment = lookupMainFragment();
        if (mainFragment == null) {
            return;
        }

        if (sensorTypes.size() != sensorTypeNames.size()) {
            Log.e(TAG, "sensorTypes.size(" + sensorTypes.size() + ") != " +
                    "sensorTypeNames.size(" + sensorTypeNames.size() + ")");
            return;
        }

        for (int i = 0, n = sensorTypes.size(); i < n; i++) {
            int sensorType = sensorTypes.get(i);
            String sensorTypeName = sensorTypeNames.get(i);
            Log.d(TAG, "SENSOR[" + (i+1) + "/" + n + "]: type(" + sensorType + ")" +
                    ": " + sensorTypeName);
        }

        if (sensorTypes.size() > 0) {
            RecyclerView recyclerView = findViewById(R.id.sensorItemList);
            if (recyclerView != null) {
                SensorItemAdapter sensorItemAdapter =
                        (SensorItemAdapter) recyclerView.getAdapter();
                if (sensorItemAdapter != null) {
                    Log.d(TAG, "SensorItemAdapter: itemCount=" +
                            sensorItemAdapter.getItemCount());
                    if (sensorItemAdapter.getItemCount() > 0) {
                        /* Activity suspend/resume case */
                        Log.d(TAG, "SensorItemAdapter has already set");
                        if (sensorItemAdapter.isAnyItemChecked()) {
                            if (mViewModel.isSensorRunning()) {
                                Log.d(TAG, "Going to enable preselected sensors");
                                onEnableSensors();
                            } else {
                                Log.d(TAG, "Sensors disabled, do nothing here");
                            }
                        } else {
                            Log.d(TAG, "No sensors have selected yet");
                        }
                    } else {
                        Log.d(TAG, "Going to register sensor items...");
                        for (int i = 0, n = sensorTypes.size(); i < n; i++) {
                            Integer sensorType = sensorTypes.get(i);
                            String sensorTypeName = sensorTypeNames.get(i);
                            sensorItemAdapter.addItem(sensorType, sensorTypeName);
                        }
                    }
                }
            } else {
                Log.w(TAG, "RecyclerView has gone?");
            }

            if (mIsWriterAvailable) {
                mainFragment.showEmptyMessage(false);
            } else {
                Log.d(TAG, "Connection attempt is not yet finished");
            }
        } else {
            Log.d(TAG, "No SensorTypes available");
            if (mIsWriterAvailable) {
                mainFragment.showEmptyMessage(true);
            } else {
                Log.d(TAG, "Connection attempt is not yet finished");
            }
        }
    }

    /**
     * Called when SensorService has bound by SensorController#bindSensorService.
     * From this point, client can operate sensors on the device.
     *
     * @param info A supplemental message from system, if any
     */
    @Override
    public void onSensorEngaged(@NonNull String info) {
        /* Implementation of SensorListener.onSensorEngaged */
        Log.d(TAG, "onSensorEngaged: " + info);

        if (mSensorController == null) {
            /*
             * For SSL/TLS connection, choose certificate Activity
             * is now running. Wait until the control comes back.
             */
            Log.d(TAG, "Wait until MainActivity comes back...");
            return;
        }

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences != null) {
            String sval1, sval2;

            /* XXX: java.lang.ClassCastException?
            long seconds = sharedPreferences.getLong(
                    getString(R.string.pref_key_sensor_interval_timer), 10L);
             */
            sval1 = sharedPreferences.getString(
                    getString(R.string.pref_key_sensor_interval_timer), "10");
            if (sval1 != null) {
                long seconds = Long.parseLong(sval1);
                mSensorController.setIntervalTimer(seconds);
            }

            String publisher = sharedPreferences.
                    getString("publisher", null);
            String note = sharedPreferences.
                    getString("note", null);
            mSensorController.setUserData(publisher, note);

            if (mPrefsLocation) {
                if (mPrefsLocationAutoUpdate) {
                    /* Latest location will be set and updated by the system */
                    Log.d(TAG, "Location: Auto-Update enabled");
                    if (mLocationCache != null) {
                        Log.d(TAG, "Location: Set initial by cached data");
                        onLocationDataReceived(mLocationCache);
                        mLocationCache = null;
                    }
                } else {
                    Log.d(TAG, "Location: Going to set FIXED values");
                    sval1 = sharedPreferences.getString(
                            getString(R.string.pref_key_location_latitude), null);
                    sval2 = sharedPreferences.getString(
                            getString(R.string.pref_key_location_longitude), null);
                    if (sval1 != null && sval2 != null) {
                        try {
                            Location location = new Location(LOCATION_PROVIDER_FIXED);
                            location.setLatitude(Double.parseDouble(sval1));
                            location.setLongitude(Double.parseDouble(sval2));
                            onLocationDataReceived(location);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid FIXED location?");
                        }
                    }
                }
            } else {
                Log.d(TAG, "Exclude location from JSON output");
            }
        }

        /* Query list of available sensor types on the device */
        mSensorController.getAvailableSensorTypes();
    }

    /**
     * Called when SensorService has unbound by SensorController#unbindSensorService.
     *
     * @param info A supplemental message from system, if any
     */
    @Override
    public void onSensorDisengaged(@NonNull String info) {
        /* Implementation of SensorListener.onSensorDisengaged */
        Log.d(TAG, "onSensorDisengaged: " + info);
        MainFragment mainFragment = lookupMainFragment();
        if (mainFragment != null) {
            mainFragment.onSensorDataReceived(getString(R.string.empty_data));
        }
    }

    @Override
    public void onSensorDataReceived(@NonNull String data) {
        /* Implementation of SensorListener.onSensorDataReceived */
        Log.d(TAG, "onSensorDataReceived");

        MainFragment mainFragment = lookupMainFragment();
        if (mainFragment != null) {
            mainFragment.onSensorDataReceived(data);
        }

        SendFragment sendFragment = lookupSendFragment();
        if (sendFragment != null) {
            if (mIsWriterAvailable) {
                Log.d(TAG, "Going to publish via SINETStream...");
                sendFragment.sendMessage(data);
            } else {
                Log.d(TAG, "Writer is NOT available");
            }
        }
    }

    @Override
    public void onWriterStatusChanged(boolean available) {
        /* Implementation of SinetStreamWriterListener.onStatusChanged */
        Log.d(TAG, "onWriterStatusChanged: available=" + available);
        mIsWriterAvailable = available;

        if (isFinishing()) {
            /* Nothing to do */
            return;
        }

        /* Stop running progress bar */
        toggleProgressBar(false);

        if (available) {
            /* Connection has established */
            if (mSensorController == null) {
                mSensorController =
                        new SensorController(this, mClientId);
                mSensorController.bindSensorService();
                Log.d(TAG, "Wait until sensors become available");
            }
        } else {
            /* Connection has closed */
            if (mSensorController != null) {
                mSensorController.unbindSensorService();
                mSensorController = null;
            }
        }
    }

    /**
     * Called when {@code publish()} has completed successfully.
     *
     * @param message  Original message for publish, not {@code null}.
     * @param userData User specified opaque object, passed by {@code publish()}.
     */
    @Override
    public void onPublished(@NonNull String message, @Nullable Object userData) {
        SendFragment sendFragment = lookupSendFragment();
        if (sendFragment != null) {
            sendFragment.onPublished(message, userData);
        }
    }

    /**
     * Called when device check for location availability has finished.
     *
     * @param isReady true if we can call {@link LocationTracker#bindLocationService}.
     */
    @Override
    public void onLocationSettingsChecked(boolean isReady) {
        Log.d(TAG, "onLocationSettingsChecked: isReady=" + isReady);

        /*
         * Mark as LocationTracker is ready to bind or not.
         * Let onResume() handle the rest of works.
         */
        mIsLocationTrackerReady = isReady;
    }

    /**
     * Called when LocationService (either GPS or FLP) has bound by
     * {@link LocationTracker#bindLocationService}.
     * <p>
     * From this point, client can receive Location notifications.
     * </p>
     *
     * @param info A supplemental message from system, if any
     */
    @Override
    public void onLocationEngaged(@NonNull String info) {
        Log.d(TAG, "onLocationEngaged: " + info);

        MainFragment mainFragment = lookupMainFragment();
        if (mainFragment != null) {
            mainFragment.updateLocationProviders(info);
            mainFragment.updateLocationValue("Tracking location...");
        }
    }

    /**
     * Called when LocaionService has unbound by
     * {@link SensorController#unbindSensorService}.
     * <p>
     * Client should wait for this notification before exit.
     * </p>
     *
     * @param info A supplemental message from system, if any
     */
    @Override
    public void onLocationDisengaged(@NonNull String info) {
        Log.d(TAG, "onLocationDisengaged: " + info);
        if (mSensorController != null) {
            Log.d(TAG, "Going to RESET location");
            mSensorController.resetLocation();
        } else {
            Log.d(TAG, "Wait resetting location until SensorController comes up");
        }

        MainFragment mainFragment = lookupMainFragment();
        if (mainFragment != null) {
            mainFragment.updateLocationValue("N/A");
        }
    }

    /**
     * Called when new Location data has received.
     *
     * @param location the {@link Location} object notified from system
     */
    @Override
    public void onLocationDataReceived(@NonNull Location location) {
        if (mSensorController != null) {
            mSensorController.setLocation(
                    location.getLatitude(), location.getLongitude());
        } else {
            /* SensorService is not yet bound. Keep location data */
            mLocationCache = location;
        }

        MainFragment mainFragment = lookupMainFragment();
        if (mainFragment != null) {
            mainFragment.updateLocationValue(dumpLocation(location));
        }
    }

    private String dumpLocation(@NonNull Location location) {
        String s = "";
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        if (location.getProvider().equals(LOCATION_PROVIDER_FIXED)) {
            s += String.format(Locale.ENGLISH, "%.6f", latitude);
            s += ", ";
            s += String.format(Locale.ENGLISH, "%.6f", longitude);
        } else {
            s += "Latitude: " + String.format(Locale.ENGLISH, "%.6f", latitude) + "\n";
            s += "Longitude: " + String.format(Locale.ENGLISH, "%.6f", longitude) + "\n";

            if (location.hasAltitude()) {
                s += "Altitude: " + location.getAltitude() + " (m)" + "\n";
            }
            if (location.hasSpeed()) {
                s += "Velocity: " + location.getSpeed() + " (m/s)" + "\n";
            }
            if (location.hasBearing()) {
                s += "Bearing: " + location.getBearing() + " (degree)" + "\n";
            }

            s += "hAccuracy: " + location.getAccuracy() + " (m)" + "\n";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (location.hasVerticalAccuracy()) {
                    s += "vAccuracy: " + location.getVerticalAccuracyMeters() + " (m)" + "\n";
                }
                if (location.hasSpeedAccuracy()) {
                    s += "sAccuracy: " + location.getSpeedAccuracyMetersPerSecond() + " (m/s)" + "\n";
                }
                if (location.hasBearingAccuracy()) {
                    s += "bAccuracy: " + location.getBearingAccuracyDegrees() + " (degree)" + "\n";
                }
            }
            s += "\n";

            long utcTime = location.getTime();
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date utcDate = new Date(utcTime);
            s += sdf.format(utcDate);

            String provider = location.getProvider();
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                Bundle bundle = location.getExtras();
                if (bundle != null) {
                    s += "\n";
                    s += "Satellites: " + bundle.getInt("satellites");
                }
            }
        }
        return s;
    }

    @Override
    public void onError(@NonNull String message) {
        /* Implementation of MainFragment */
        /* Implementation of SendFragment */
        /* Implementation of SinetStreamWriterListener */
        /* Implementation of LocationTrackerListener */
        /* Implementation of SensorListener */
        Log.e(TAG, "onError: " + message);

        toggleProgressBar(false);
        DialogUtil.showErrorDialog(
                this, message, null, true);

        /*
         * If user pressed OK button on the error dialog window,
         * ErrorDialogFragment.onErrorDialogDismissed() will be called.
         */
    }

    @Override
    public void onErrorDialogDismissed(
            @Nullable Parcelable parcelable, boolean isFatal) {
        /* Implementation of ErrorDialogFragment.onErrorDialogDismissed */

        toggleProgressBar(false);
        if (isFatal) {
            onBackPressed();
        }
    }

    /**
     * Called when the activity has detected the user's press of the back
     * key. The {@link #getOnBackPressedDispatcher() OnBackPressedDispatcher} will be given a
     * chance to handle the back button before the default behavior of
     * {@link Activity#onBackPressed()} is invoked.
     *
     * @see #getOnBackPressedDispatcher()
     */
    @Override
    public void onBackPressed() {
        /*
         * It seems strange, but calling super.onBackPressed() seems to
         * nullify the setResult() effect.
         *
        super.onBackPressed();
         */
        Log.i(TAG, "Going to finish myself...");
        Intent intent = new Intent();
        intent.putExtra(ActivityCodes.KEY, ActivityCodes.ACTIVITY_CODE_MAIN);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void toggleProgressBar(boolean enabled) {
        /*
         * NB:
         * This method might be called from a non-main thread.
         * Explicitly specify a looper for the constructor of Handler().
         *
         * > ViewRoot$CalledFromWrongThreadException:
         * > Only the original thread that created a view hierarchy
         * > can touch its views.
         */
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                MainFragment mainFragment = lookupMainFragment();
                if (mainFragment != null) {
                    mainFragment.showProgressBar(enabled);
                }
            }
        });
    }
}
