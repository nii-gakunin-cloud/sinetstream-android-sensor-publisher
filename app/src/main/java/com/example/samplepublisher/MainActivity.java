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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.samplepublisher.ui.configserver.SharedPrefsAccessKey;
import com.example.samplepublisher.ui.configserver.SharedPrefsConfigServer;
import com.example.samplepublisher.ui.dialogs.ErrorDialogFragment;
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
import jp.ad.sinet.stream.android.config.remote.ConfigServerSettings;
import jp.ad.sinet.stream.android.helper.CellularMonitor;
import jp.ad.sinet.stream.android.helper.CellularMonitorListener;
import jp.ad.sinet.stream.android.helper.LocationTracker;
import jp.ad.sinet.stream.android.helper.LocationTrackerListener;
import jp.ad.sinet.stream.android.helper.PermissionHandler;
import jp.ad.sinet.stream.android.helper.SensorController;
import jp.ad.sinet.stream.android.helper.SensorListener;
import jp.ad.sinet.stream.android.helper.constants.LocationProviderType;
import jp.ad.sinet.stream.android.helper.constants.PermissionTypes;
import jp.ad.sinet.stream.android.net.cert.KeyChainHandler;

public class MainActivity extends AppCompatActivity implements
        MainFragment.OnFragmentInteractionListener,
        SendFragment.SendFragmentListener,
        SinetStreamWriterString.SinetStreamWriterStringListener,
        ErrorDialogFragment.ErrorDialogListener,
        LocationTrackerListener,
        CellularMonitorListener,
        SensorListener {
    private final String TAG = MainActivity.class.getSimpleName();

    private boolean mIsPermissionCheckStarted = false;
    private boolean mIsPermissionCheckFinished = false;
    private PermissionHandler mPermissionHandler = null;

    /* Sensor handling stuff */
    private SensorViewModel mViewModel = null;
    private final int mClientId = 1;
    private SensorController mSensorController = null;
    private ArrayList<Integer> mDeniedSensorTypes = null;
    private ArrayList<Integer> mRunningSensorTypes = null;

    /* Cellular handling stuff */
    private CellularMonitor mCellularMonitor = null;
    private boolean mIsCellularDebug = false;
    private boolean mIsCellularMonitorReady = false;
    private boolean mPrefsCellular = false;
    private Bundle mCellularCache = null;

    /* Location handling stuff */
    private LocationTracker mLocationTracker = null;
    private boolean mIsLocationDebug = false;
    private boolean mIsLocationTrackerReady = false;
    private boolean mPrefsLocation = false;
    private boolean mPrefsLocationAutoUpdate = false;
    private String mLocationProvider = null;
    private final String LOCATION_PROVIDER_FIXED = "fixed";
    private Location mLocationCache = null;

    private boolean mIsWriterAvailable = false;

    private SharedPreferences mSharedPreferences;

    /* Parameters to be required for the remote configuration server access */
    private boolean mUseConfigServer = false;
    private boolean mIsProtocolDebug = false;
    private ConfigServerSettings mConfigServerSettings = null;
    private String mServerUrl = null;
    private String mAccount = null;
    private String mSecretKey = null;
    private boolean mIsAccessTokenLoaded = false;

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

                mUseConfigServer = bundle.getBoolean(
                        BundleKeys.BUNDLE_KEY_USE_CONFIG_SERVER, false);
                mIsProtocolDebug = bundle.getBoolean(
                        BundleKeys.BUNDLE_KEY_PROTOCOL_DEBUG, false);
                mIsCellularDebug = bundle.getBoolean(
                        BundleKeys.BUNDLE_KEY_CELLULAR_DEBUG, false);
                mIsLocationDebug = bundle.getBoolean(
                        BundleKeys.BUNDLE_KEY_LOCATION_DEBUG, false);

                if (mUseConfigServer) {
                    /*
                     * Let user pickup an AccessToken which must have downloaded
                     * on this device.
                     * Note that series of dialogs may appear during the remote
                     * configuration processes, if there are multiple choices in
                     * the SINETStream configuration set.
                     */
                    setupRemoteConfiguration();
                } else {
                    Log.d(TAG, "Use manually chosen SINETStream configurations");
                }
            }

            /* Check permissions both for system and application run-time */
            checkPrefsLocationSettings();
            checkPrefsCellularSettings();

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            MainFragment mainFragment = new MainFragment();
            if (mPrefsCellular || mPrefsLocation) {
                Bundle bundle1 = new Bundle();
                if (mPrefsCellular) {
                    bundle1.putBoolean(BundleKeys.BUNDLE_KEY_CELLULAR, true);
                    bundle1.putBoolean(BundleKeys.BUNDLE_KEY_CELLULAR_DEBUG, mIsCellularDebug);
                }
                if (mPrefsLocation) {
                    final String provider;
                    if (mPrefsLocationAutoUpdate) {
                        provider = mLocationProvider;
                        bundle1.putBoolean(BundleKeys.BUNDLE_KEY_LOCATION_DEBUG, mIsLocationDebug);
                    } else {
                        provider = LOCATION_PROVIDER_FIXED;
                    }
                    bundle1.putString(BundleKeys.BUNDLE_KEY_LOCATION_PROVIDER,
                            provider.toUpperCase(Locale.US));
                }
                mainFragment.setArguments(bundle1);
            }
            if (mPrefsCellular || mPrefsLocationAutoUpdate) {
                setFragmentOnAttachListener(fragmentManager);
            }
            transaction.replace(R.id.container, mainFragment,
                    MainFragment.class.getSimpleName());

            /*
             * Since we don't have to control the SINETStream Writer module
             * via specific UI, we allocate the SendFragment as a UI-less
             * worker module.
             */
            SendFragment sendFragment = new SendFragment();
            Bundle bundle2 = ((bundle != null) ? bundle : new Bundle());
            bundle2.putBoolean(BundleKeys.BUNDLE_KEY_PROTOCOL_DEBUG, mIsProtocolDebug);
            sendFragment.setArguments(bundle2);
            transaction.add(sendFragment,
                    SendFragment.class.getSimpleName());

            transaction.commit();
        }
        registerPermissionHandler();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        clearFragmentOnAttachListener();
        clearRemoteConfiguration();
        super.onDestroy();
    }

    private void checkPrefsCellularSettings() {
        String key = getString(R.string.pref_key_toggle_cellular);
        if (mSharedPreferences.getBoolean(key, false)) {
            mPrefsCellular = true;
            Log.d(TAG, "Cellular: ENABLED");
        } else {
            Log.d(TAG, "Cellular: DISABLED");
        }
    }

    private void checkPrefsLocationSettings() {
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

    @Nullable
    private String getLocationProviderName() {
        if (mLocationProvider != null) {
            if (mLocationProvider.equals(LocationManager.GPS_PROVIDER)) {
                return LocationManager.GPS_PROVIDER;
            }
            if (mLocationProvider.equals(LocationManager.FUSED_PROVIDER)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return LocationManager.FUSED_PROVIDER;
                } else {
                    return "fused";
                }
            }
            Log.w(TAG, "Unknown LocationProvider: " + mLocationProvider);
        }
        return null;
    }

    private void registerPermissionHandler() {
        /*
         * Avoid IllegalStateException:
         * LifecycleOwners must call register before they are STARTED.
         */
        mPermissionHandler =
                new PermissionHandler(this,
                        new PermissionHandler.PermissionHandlerListener() {
                            @Override
                            public void onPermissionChecked(int grantedTypes, int deniedTypes) {
                                Log.d(TAG, "onPermissionCheckFinished");
                                mIsPermissionCheckFinished = true;

                                if ((deniedTypes &
                                        PermissionTypes.ACTIVITY_RECOGNITION) != 0) {
                                    Log.w(TAG, "Some sensor data unavailable");
                                    mDeniedSensorTypes = mPermissionHandler.getDeniedSensorTypes();
                                }
                                if (mPrefsCellular) {
                                    if ((grantedTypes &
                                            PermissionTypes.READ_PHONE_STATE) != 0) {
                                        onCellularSettingsChecked(true);
                                    } else
                                    if ((deniedTypes &
                                            PermissionTypes.READ_PHONE_STATE) != 0) {
                                        Log.w(TAG, "Cellular data unavailable");
                                        mPrefsCellular = false;
                                    }
                                }
                                if (mPrefsLocationAutoUpdate) {
                                    if ((grantedTypes &
                                            PermissionTypes.LOCATION) != 0) {
                                        onLocationSettingsChecked(true);
                                    } else
                                    if ((deniedTypes &
                                            PermissionTypes.LOCATION) != 0) {
                                        Log.w(TAG, "Location data unavailable");
                                        mPrefsLocationAutoUpdate = false;
                                    }
                                }
                                onResume();
                            }

                            @Override
                            public void onError(@NonNull String description) {
                                MainActivity.this.onError(description);
                            }
                        });

        mPermissionHandler.checkSensorPermissions();
        if (mPrefsLocationAutoUpdate) {
            String providerName = getLocationProviderName();
            if (providerName != null) {
                mPermissionHandler.checkLocationPermissions(providerName);
            }
        }
        if (mPrefsCellular) {
            mPermissionHandler.checkCellularPermissions();
        }
    }

    private void checkPermissions() {
        mIsPermissionCheckStarted = true;
        mPermissionHandler.run();
        /*
         * State transition after calling PermissionHandler.run()
         * would look as follows.
         *
         * <MainActivity>
         *     onPause()
         *     onStop()
         *     ...    <-- System Settings
         *     onResume()
         */
    }

    private void setFragmentOnAttachListener(@NonNull FragmentManager fragmentManager) {
        Log.d(TAG, "setFragmentOnAttachListener");
        /*
         * Now that "Activity.onAttachFragment()" has deprecated, we handle the
         * fragment attach event by
         * {@link FragmentManager#addFragmentOnAttachListener(FragmentOnAttachListener)}.
         */
        fragmentManager.addFragmentOnAttachListener(new FragmentOnAttachListener() {
            @Override
            public void onAttachFragment(
                    @NonNull FragmentManager fragmentManager,
                    @NonNull Fragment fragment) {
                Log.d(TAG, "onAttachFragment");
                if (fragment instanceof MainFragment) {
                    if (mPrefsCellular) {
                        Log.d(TAG, "Going to start CellularMonitor...");
                        mCellularMonitor = new CellularMonitor(
                                MainActivity.this, mClientId);
                        mCellularMonitor.start();
                    }

                    if (mPrefsLocationAutoUpdate) {
                        Log.d(TAG, "Going to start LocationTracker...");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            boolean useGpsProvider =
                                    (mLocationProvider != null
                                            && mLocationProvider.equals(
                                            getString(R.string.pref_default_location_provider)));

                            if (useGpsProvider) {
                                mLocationTracker =
                                        new LocationTracker(
                                                MainActivity.this,
                                                LocationProviderType.GPS,
                                                mClientId);
                            } else {
                                mLocationTracker =
                                        new LocationTracker(
                                                MainActivity.this,
                                                LocationProviderType.FUSED,
                                                mClientId);
                            }
                        } else {
                            /* GPS only */
                            mLocationTracker =
                                    new LocationTracker(
                                            MainActivity.this,
                                            LocationProviderType.GPS,
                                            mClientId);
                        }
                        mLocationTracker.start();
                    }
                }
            }
        });
    }

    private void clearFragmentOnAttachListener() {
        Log.d(TAG, "clearFragmentOnAttachListener");
        if (mCellularMonitor != null) {
            mCellularMonitor.stop();
            mCellularMonitor = null;
        }

        if (mLocationTracker != null) {
            mLocationTracker.stop();
            mLocationTracker = null;
        }
    }

    private void setupRemoteConfiguration() {
        Log.d(TAG, "setupRemoteConfiguration");

        /* If preloaded AccessToken exists, use it. */
        SharedPrefsAccessKey sharedPrefsAccessKey =
                new SharedPrefsAccessKey(this);

        if (sharedPrefsAccessKey.isAccessTokenEmpty()) {
            Log.d(TAG, "Preloaded AccessToken does not exist");
        } else {
            Log.d(TAG, "Going to use preloaded AccessToken");
            if (sharedPrefsAccessKey.isAccessTokenExpired()) {
                onError(getString(R.string.auth_json_expired));
                return;
            }
            mServerUrl = sharedPrefsAccessKey.readAccessTokenServerUrl();
            mAccount = sharedPrefsAccessKey.readAccessTokenAccount();
            mSecretKey = sharedPrefsAccessKey.readAccessTokenSecretKey();
            mIsAccessTokenLoaded = true;
            return;
        }

        /* Load an AccessToken interactively */
        mConfigServerSettings = new ConfigServerSettings(this,
                new ConfigServerSettings.ConfigServerSettingsListener() {
                    /**
                     * Called when user-specified settings file (auth.json) contains valid
                     * parameter values.
                     *
                     * @param serverUrl      The URL of the configuration server.
                     * @param account        The login account for the configuration server.
                     * @param secretKey      The API key published by the configuration server.
                     * @param expirationDate Expiration date of the secretKey.
                     */
                    @Override
                    public void onParsed(@NonNull String serverUrl,
                                         @NonNull String account,
                                         @NonNull String secretKey,
                                         @NonNull Date expirationDate) {
                        mServerUrl = serverUrl;
                        mAccount = account;
                        mSecretKey = secretKey;
                        /*
                         * Don't care expirationDate here.
                         * Let onExpired() handle the event instead.
                         */

                        mIsAccessTokenLoaded = true;
                    }

                    @Override
                    public void onExpired() {
                        MainActivity.this.onError(
                                getString(R.string.auth_json_expired));
                    }

                    @Override
                    public void onError(@NonNull String description) {
                        MainActivity.this.onError(description);
                    }
                });

        mConfigServerSettings.launchDocumentPicker();
        /*
         * Expected call flow in this case:
         *
         * <ConfigServerSettings>
         *     ActivityResultLauncher.launch()
         * <MainActivity>
         *     onPause()
         *     onStop()
         *     ...           <-- DocumentPicker
         *     onStart()
         * <ConfigServerSettings>
         *     ActivityResultLauncher.onActivityResult()
         * <MainActivity>
         *     onResume()
         */
    }

    private void clearRemoteConfiguration() {
        Log.d(TAG, "clearRemoteConfiguration");
        if (mConfigServerSettings != null) {
            mConfigServerSettings.clearDocumentPicker();
            mConfigServerSettings = null;
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
                findFragmentByTag(MainFragment.class.getSimpleName());
        if (fragment == null) {
            Log.e(TAG, "MainFragment not found?");
        }
        return fragment;
    }

    @Nullable
    private SendFragment lookupSendFragment() {
        SendFragment fragment;
        fragment = (SendFragment) getSupportFragmentManager().
                findFragmentByTag(SendFragment.class.getSimpleName());
        if (fragment == null) {
            Log.e(TAG, "SendFragment not found?");
        }
        return fragment;
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();

        if (mUseConfigServer) {
            Log.d(TAG, "Use ConfigServer, nothing to do here");
            return;
        } else {
            Log.d(TAG, "Manual configuration");
            if (mIsPermissionCheckFinished) {
                Log.d(TAG, "Going to start fragments");
            } else {
                Log.d(TAG, "Going to check runtime permissions");
                checkPermissions();
                return;
            }
        }

        if (getPrefsToggleSslTls()) {
            /* Use SSL/TLS */
            if (getPrefsClientCertificates()) {
                /* Use Client Certificate */
                String alias = mViewModel.getPrivateKeyAlias();
                if (alias != null) {
                    Log.d(TAG, "Re-use certificate alias: " + alias);
                    runFragments(alias);
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
                                        runFragments(alias);
                                    } else {
                                        onError(getString(R.string.keychain_alias_unspecified));
                                    }
                                }
                            });
                }
            } else {
                /* Don't use Client Certificate */
                runFragments(null);
            }
        } else {
            /* Don't use SSL/TLS */
            runFragments(null);
        }
    }

    private void runFragments(@Nullable String alias) {
        Log.d(TAG, "runFragments: alias(" + alias + ")");

        SendFragment sendFragment = lookupSendFragment();

        if (mUseConfigServer) {
            /*
             * Automatic list item selection:
             * During the configuration server sessions, there may be a case
             * in which multiple choices are being presented depending on
             * the configuration content.
             * Usually, user will have to choose the desired item on the fly,
             * but also user can skip such interventions by specifying the
             * selection items beforehand.
             */
            SharedPrefsConfigServer sharedPrefsConfigServer =
                    new SharedPrefsConfigServer(this);
            String dataStream = sharedPrefsConfigServer.getDataStream();
            String serviceName = sharedPrefsConfigServer.getServiceName();

            if (sendFragment != null) {
                if (mServerUrl != null && mAccount != null && mSecretKey != null) {
                    /* Download SINETStream settings from configuration server */
                    sendFragment.setRemoteConfig(
                            mServerUrl, mAccount, mSecretKey);
                }
                if (dataStream != null && serviceName != null) {
                    /* Specify multiple-choice items for automatic selection */
                    sendFragment.setPredefinedParameters(
                            dataStream, serviceName);
                }
            }
        }
        if (sendFragment != null) {
            sendFragment.initializeWriter(alias);
        }
        toggleProgressBar(true);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");

        SendFragment sendFragment = lookupSendFragment();
        if (sendFragment != null) {
            /* Prevent timing-dependent NullPointerException */
            if (mIsWriterAvailable) {
                sendFragment.terminateWriter();
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

        if (mUseConfigServer) {
            Log.d(TAG, "Use ConfigServer");
            if (mIsAccessTokenLoaded) {
                Log.d(TAG, "AccessToken has loaded");
                if (mIsPermissionCheckFinished) {
                    Log.d(TAG, "Going to run fragments");
                    runFragments(null);
                } else {
                    if (mIsPermissionCheckStarted) {
                        Log.d(TAG, "Wait until PermissionCheck to finish");
                    } else {
                        Log.d(TAG, "Going to check permissions");
                        checkPermissions();
                    }
                    return;
                }
            } else {
                Log.d(TAG, "Wait for the AccessToken to be loaded");
                return;
            }
        } else {
            Log.d(TAG, "Use local configuration");
            if (mIsPermissionCheckFinished) {
                Log.d(TAG, "Going to run fragments");
                runFragments(null);
            } else {
                if (mIsPermissionCheckStarted) {
                    Log.d(TAG, "Wait until PermissionCheck to finish");
                } else {
                    Log.d(TAG, "Going to check permissions");
                    checkPermissions();
                }
                return;
            }
        }

        /* Bind LocationService to receive location updates */
        if (mLocationTracker != null) {
            if (mIsLocationTrackerReady) {
                Log.d(TAG, "Going to bind LocationService");
                mLocationTracker.bindLocationService();
            } else {
                Log.d(TAG, "Wait until LocationTracker gets ready");
            }
        }

        /* Bind CellularService to receive signal strength updates */
        if (mCellularMonitor != null) {
            if (mIsCellularMonitorReady) {
                Log.d(TAG, "Going to bind CellularService");
                mCellularMonitor.bindCellularService();
            } else {
                Log.d(TAG, "Wait until CellularMonitor gets ready");
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

        /* Unbind CellularService to stop receiving signal strength updates */
        if (mCellularMonitor != null) {
            Log.d(TAG, "Going to unbind CellularService");
            mCellularMonitor.unbindCellularService();
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
                sensorItemAdapter.enableAllSensorTypes(false);
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
                sensorItemAdapter.enableAllSensorTypes(true);
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
                    getString(R.string.pref_key_sensor_interval_timer),
                    getString(R.string.pref_default_sensor_interval_timer));
            try {
                long duration = Long.parseLong(sval1); /* unit: 100ms */
                long milliseconds = duration * 100;
                mSensorController.setIntervalTimer(milliseconds);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid interval timer: " + e.getMessage());
            }

            String publisher = sharedPreferences.
                    getString("publisher", null);
            String note = sharedPreferences.
                    getString("note", null);
            mSensorController.setUserData(publisher, note);

            if (mPrefsCellular) {
                if (mCellularCache != null) {
                    Log.d(TAG, "Cellular: Set initial by cached data");
                    onCellularDataReceived(mCellularCache);
                    mCellularCache = null;
                }
            }
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

        if (mIsWriterAvailable) {
            SendFragment sendFragment = lookupSendFragment();
            if (sendFragment != null) {
                sendFragment.sendMessage(data);
            }
        }
    }

    /**
     * Called when initialization process, including configuration loading,
     * has finished.
     * Now user can call "SinetStreamWriter<T>.setup()" next.
     */
    @Override
    public void onWriterConfigLoaded() {
        SendFragment sendFragment = lookupSendFragment();
        if (sendFragment != null) {
            sendFragment.setupWriter();
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
                if (mDeniedSensorTypes != null) {
                    mSensorController.setExcludeSensorTypes(mDeniedSensorTypes);
                }
                mSensorController.bindSensorService();
                Log.d(TAG, "Wait until sensors become available");
            } else {
                Log.d(TAG, "Reconnect completed, Going to resume publish");
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
     * Called when the broker connection has lost and auto-reconnect
     * procedure is in progress.
     */
    @Override
    public void onWriterReconnectInProgress() {
        /* Implementation of SinetStreamWriterListener.onWriterReconnectInProgress */
        Log.d(TAG, "onWriterReconnectInProgress");
        mIsWriterAvailable = false;
        toggleProgressBar(true);
    }

    /**
     * Called when {@code publish()} has completed successfully.
     *
     * @param message  Original message for publish, not {@code null}.
     * @param userData User specified opaque object, passed by {@code publish()}.
     */
    @Override
    public void onPublished(@NonNull String message, @Nullable Object userData) {
        /* Implementation of SinetStreamWriterListener.onPublished */
        MainFragment mainFragment = lookupMainFragment();
        if (mainFragment != null) {
            mainFragment.onSensorDataReceived(message);
        }
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
                    location.getLatitude(), location.getLongitude(), location.getTime());
        } else {
            /* SensorService is not yet bound. Keep location data */
            mLocationCache = location;
        }

        MainFragment mainFragment = lookupMainFragment();
        if (mainFragment != null) {
            mainFragment.updateLocationValue(dumpLocation(location));
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private String dumpLocation(@NonNull Location location) {
        String s = "";
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        if (location.getProvider().equals(LOCATION_PROVIDER_FIXED) || !mIsLocationDebug) {
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

    /**
     * Called when device check for cellular network availability has finished.
     *
     * @param isReady true if we can call {@link CellularMonitor#bindCellularService}.
     */
    @Override
    public void onCellularSettingsChecked(boolean isReady) {
        Log.d(TAG, "onCellularSettingsChecked: isReady=" + isReady);

        /*
         * Mark as CellularMonitor is ready to bind or not.
         * Let onResume() handle the rest of works.
         */
        mIsCellularMonitorReady = isReady;
    }

    /**
     * Called when new cellular network data has received.
     *
     * @param bundle the telephony data to be passed to {@link SensorController}
     */
    @Override
    public void onCellularDataReceived(@NonNull Bundle bundle) {
        if (mSensorController != null) {
            mSensorController.setCellularData(bundle);
        } else {
            /* SensorService is not yet bound. Keep cellular data */
            mCellularCache = bundle;
        }

        mCellularMonitor.getNetworkSummary(bundle);
    }

    @Override
    public void onCellularSummary(@NonNull String networkType, @NonNull String data) {
        MainFragment mainFragment = lookupMainFragment();
        if (mainFragment != null) {
            mainFragment.updateCellularInfo(networkType, data);
        }
    }

    @Override
    public void onError(@NonNull String description) {
        /* Implementation of MainFragment */
        /* Implementation of SendFragment */
        /* Implementation of SinetStreamWriterListener */
        /* Implementation of LocationTrackerListener */
        /* Implementation of SensorListener */
        Log.e(TAG, "onError: " + description);

        onWriterStatusChanged(false);
        toggleProgressBar(false);
        DialogUtil.showErrorDialog(
                this, description, null, true);

        /*
         * If user pressed OK button on the error dialog window,
         * ErrorDialogFragment.onErrorDialogDismissed() will be called.
         */
    }

    @Override
    public void onErrorDialogDismissed(boolean isFatal) {
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
