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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplepublisher.net.SinetStreamWriterString;
import com.example.samplepublisher.ui.main.ErrorDialogFragment;
import com.example.samplepublisher.ui.main.InProgressDialogFragment;
import com.example.samplepublisher.ui.main.MainFragment;
import com.example.samplepublisher.ui.main.SendFragment;
import com.example.samplepublisher.ui.main.SensorItemAdapter;
import com.example.samplepublisher.util.DialogUtil;

import java.util.ArrayList;

import jp.ad.sinet.stream.android.helper.SensorController;
import jp.ad.sinet.stream.android.helper.SensorListener;

public class MainActivity extends AppCompatActivity implements
        MainFragment.OnFragmentInteractionListener,
        SendFragment.SendFragmentListener,
        SinetStreamWriterString.SinetStreamWriterStringListener,
        InProgressDialogFragment.ProgressDialogListener,
        ErrorDialogFragment.ErrorDialogListener,
        SensorListener {
    private final String TAG = MainActivity.class.getSimpleName();

    private final int mSensorClientId = 1;
    private SensorController mSensorController = null;
    private ArrayList<Integer> mRunningSensorTypes = null;
    private InProgressDialogFragment mInProgressDialogFragment = null;

    private boolean mIsWriterAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

            FragmentTransaction transaction =
                    getSupportFragmentManager().beginTransaction();
            MainFragment mainFragment = new MainFragment();
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

        SendFragment sendFragment = lookupSendFragment();
        if (sendFragment != null) {
            sendFragment.startWriter();

            mInProgressDialogFragment =
                    new InProgressDialogFragment(
                            getString(R.string.dialog_title_connecting));
            mInProgressDialogFragment.show(
                    getSupportFragmentManager(), null);
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");

        SendFragment sendFragment = lookupSendFragment();
        if (sendFragment != null) {
            sendFragment.stopWriter();
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

        mSensorController = new SensorController(this, mSensorClientId);
        mSensorController.bindSensorService();
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        if (mSensorController != null) {
            mSensorController.unbindSensorService();
            mSensorController = null;
        }
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
    public void onEnableSensors(boolean enable) {
        /* Implementation of MainFragment.onEnableSensors */
        Log.d(TAG, "onEnableSensors: " + enable);

        RecyclerView recyclerView = findViewById(R.id.sensorItemList);
        if (recyclerView != null) {
            SensorItemAdapter sensorItemAdapter =
                    (SensorItemAdapter) recyclerView.getAdapter();
            if (sensorItemAdapter != null) {
                if (enable) {
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
                    } else {
                        /* Display rotation case */
                        Log.d(TAG, "SensorController is still OFFLINE");
                    }

                    /* Prevent touching sensor list while running */
                    sensorItemAdapter.enableSensorTypes(false);
                } else {
                    if (mSensorController != null) {
                        mSensorController.disableSensors(mRunningSensorTypes);
                    } else {
                        /* Display rotation case */
                        Log.d(TAG, "SensorController is still OFFLINE");
                    }
                    mRunningSensorTypes = null;

                    /* Enable sensor list again */
                    sensorItemAdapter.enableSensorTypes(true);
                }
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
                    if (sensorItemAdapter.isAnyItemChecked()) {
                        /* Activity suspend/resume case */
                        Log.d(TAG, "Going to enable preselected sensors");
                        onEnableSensors(true);
                    } else {
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
            mainFragment.showEmptyMessage(false);
        } else {
            Log.d(TAG, "No SensorTypes available");
            mainFragment.showEmptyMessage(true);
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

            /* XXX: java.lang.ClassCastException???
            float longitude = sharedPreferences.
                    getFloat(getString(R.string.pref_key_location_longitude), Float.NaN);
            float latitude = sharedPreferences.
                    getFloat(getString(R.string.pref_key_location_latitude), Float.NaN);
             */
            sval1 = sharedPreferences.getString(
                    getString(R.string.pref_key_location_longitude), null);
            sval2 = sharedPreferences.getString(
                    getString(R.string.pref_key_location_latitude), null);
            if (sval1 != null && sval2 != null) {
                float longitude = Float.parseFloat(sval1);
                float latitude = Float.parseFloat(sval2);
                mSensorController.setLocation(longitude, latitude);
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
            }
        }
    }

    @Override
    public void onWriterStatusChanged(boolean available) {
        /* Implementation of SinetStreamWriterListener.onStatusChanged */
        Log.d(TAG, "onWriterStatus Changed: available=" + available);
        mIsWriterAvailable = available;

        if (mInProgressDialogFragment != null) {
            mInProgressDialogFragment.dismiss();
            mInProgressDialogFragment = null;
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

    @Override
    public void onError(@NonNull String message) {
        /* Implementation of MainFragment */
        /* Implementation of SendFragment */
        /* Implementation of SinetStreamWriterListener */
        /* Implementation of SensorListener */
        Log.e(TAG, "onError: " + message);

        DialogUtil.showErrorDialog(
                this, message, null, true);
    }

    @Override
    public void onErrorDialogDismissed(
            @Nullable Parcelable parcelable, boolean isFatal) {
        /* Implementation of ErrorDialogFragment.ErrorDialogListener */

        if (isFatal) {
            if (mInProgressDialogFragment != null) {
                mInProgressDialogFragment.dismiss();
                mInProgressDialogFragment = null;
            }
            Log.i(TAG, "Going to finish myself...");
            finish();
        }
    }

    @Override
    public void onCanceled() {
        /* Implementation of InProgressDialogFragment.ProgressDialogListener */
        Log.d(TAG, "onCanceled");
    }
}
