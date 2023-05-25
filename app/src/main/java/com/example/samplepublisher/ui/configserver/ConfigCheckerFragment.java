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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleObserver;

import com.example.samplepublisher.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jp.ad.sinet.stream.android.config.remote.RemoteConfigLoader;

public class ConfigCheckerFragment extends Fragment {
    private final String TAG = ConfigCheckerFragment.class.getSimpleName();

    private Context mContext = null;
    private ConfigCheckerListener mListener = null;
    private TextView mTextViewConfigContents = null;

    private boolean mIsDryRun = false;

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     *
     * @param context The {@link Context} which implements
     *                the {@link ConfigCheckerListener}.
     * @throws RuntimeException if the context argument is not
     *                          an instance of {@link ConfigCheckerListener}.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        if (context instanceof ConfigCheckerListener) {
            mListener = (ConfigCheckerListener) context;
        } else {
            throw new RuntimeException(context +
                    " must implement ConfigCheckerListener");
        }
    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(Activity)} and before
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     *
     * <p>Note that this can be called while the fragment's activity is
     * still in the process of being created.  As such, you can not rely
     * on things like the activity's content view hierarchy being initialized
     * at this point.  If you want to do work once the activity itself is
     * created, add a {@link LifecycleObserver} on the
     * activity's Lifecycle, removing it when it receives the
     * {Lifecycle.State#CREATED} callback.
     *
     * <p>Any restored child fragments will be created before the base
     * <code>Fragment.onCreate</code> method returns.</p>
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * [NB]
         * Arguments are provided from a Preference "extra" part
         * defined in "res/xml/settings_config_server.xml".
         */
        Bundle arguments = getArguments();
        if (arguments != null) {
            mIsDryRun = arguments.getBoolean("isDryRun", false);
            Log.d(TAG, "isDryRun=" + mIsDryRun);
        }

        SharedPrefsAccessKey sharedPrefsAccessKey =
                new SharedPrefsAccessKey(mContext);
        String serverUrl = sharedPrefsAccessKey.readAccessTokenServerUrl();
        String account = sharedPrefsAccessKey.readAccessTokenAccount();
        String secretKey = sharedPrefsAccessKey.readAccessTokenSecretKey();

        if (serverUrl != null && account != null && secretKey != null) {
            runRemoteConfigLoader(serverUrl, account, secretKey);
        } else {
            mListener.onError(TAG + ": Cannot read SharedPreference");
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null. This will be called between
     * {@link #onCreate(Bundle)} and {@link #onViewCreated(View, Bundle)}.
     * <p>A default View can be returned by calling {#Fragment(int)} in your
     * constructor. Otherwise, this method returns null.
     *
     * <p>It is recommended to <strong>only</strong> inflate the layout in this method and move
     * logic that operates on the returned View to {@link #onViewCreated(View, Bundle)}.
     *
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(
                R.layout.fragment_config_checker, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.  The fragment's
     * view hierarchy is not however attached to its parent at this point.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTextViewConfigContents = view.findViewById(R.id.textview_config_contents);
    }

    private void runRemoteConfigLoader(@NonNull String serverUrl,
                                       @NonNull String account,
                                       @NonNull String secretKey) {
        RemoteConfigLoader remoteConfigLoader =
                new RemoteConfigLoader(serverUrl, account, secretKey);
        SharedPrefsConfigServer sharedPrefsConfigServer =
                new SharedPrefsConfigServer(mContext);

        if (mIsDryRun) {
            String dataStream = sharedPrefsConfigServer.getDataStream();
            String serviceName = sharedPrefsConfigServer.getServiceName();
            if (dataStream != null && serviceName != null) {
                Log.d(TAG, "Going to REUSE ConfigServer parameters");
                remoteConfigLoader.setPredefinedParameters(dataStream, serviceName);
            }
        }

        showProgressBar(true);
        remoteConfigLoader.peek(mContext, TAG,
                new RemoteConfigLoader.RemoteConfigPeekListener() {
                    @Override
                    public void onRawData(@NonNull String name,
                                          @NonNull String service,
                                          @Nullable JSONObject header,
                                          @NonNull JSONObject config,
                                          @NonNull JSONArray attachments,
                                          @Nullable JSONArray secrets) {
                        showProgressBar(false);

                        /* Keep some parameters in the shared preference */
                        sharedPrefsConfigServer.writeConfigServerPrefs(name, service);

                        if (mTextViewConfigContents != null) {
                            String s = "";

                            s += "--HEADER--" + "\n";
                            if (header != null) {
                                try {
                                    s += "header: " + header.toString(4) + "\n";
                                } catch (JSONException e) {
                                    mListener.onError(TAG + ": Invalid Header: " + e.getMessage());
                                    return;
                                }
                            }

                            s += "--CONFIG--" + "\n";
                            s += "name: " + name + "\n";
                            s += "service: " + service + "\n";
                            try {
                                s += "config: " + config.toString(4) + "\n";
                            } catch (JSONException e) {
                                mListener.onError(TAG + ": Invalid config: " + e.getMessage());
                                return;
                            }
                            mTextViewConfigContents.setText(s);
                            mTextViewConfigContents.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(@NonNull String description) {
                        mListener.onError(description);
                    }
                });
    }

    private void showProgressBar(boolean enabled) {
        View rootView = getView();
        if (rootView != null) {
            LinearLayout linearLayout = rootView.findViewById(R.id.progressBar);
            if (linearLayout != null) {
                linearLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
            }
        }
    }

    public interface ConfigCheckerListener {
        void onError(@NonNull String description);
    }
}
