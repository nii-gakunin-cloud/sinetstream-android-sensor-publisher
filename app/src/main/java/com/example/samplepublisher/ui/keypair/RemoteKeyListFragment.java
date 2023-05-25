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

package com.example.samplepublisher.ui.keypair;

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
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplepublisher.R;
import com.example.samplepublisher.constants.BundleKeys;
import com.example.samplepublisher.models.RemoteKeyItem;
import com.example.samplepublisher.ui.configserver.SharedPrefsAccessKey;
import com.example.samplepublisher.ui.dialogs.ConfirmDialogFragment;
import com.example.samplepublisher.ui.dialogs.InfoDialogFragment;

import java.util.HashSet;

import jp.ad.sinet.stream.android.config.remote.configclient.api.PubKeyClient;
import jp.ad.sinet.stream.android.config.remote.configclient.model.RemotePubKey;

public class RemoteKeyListFragment extends Fragment {
    private final String TAG = RemoteKeyListFragment.class.getSimpleName();

    private Context mContext;
    private RemoteKeyItemAdapter mRemoteKeyItemAdapter;
    private RemoteKeyListListener mListener = null;

    private String mServerUrl = null;
    private String mAccount = null;
    private String mSecretKey = null;

    private PubKeyClient mPubKeyClient;
    private String mAccessToken;

    private final String FINGERPRINT_PREFIX = "SHA256:";
    private HashSet<String> mFingerprints = null;

    private final boolean mGetAccessTokenFromSharedPreferences = true;

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     *
     * @param context The Context which implements RemoteKeyListListener
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof RemoteKeyListListener) {
            mContext = context;
            mListener = (RemoteKeyListListener) context;
        } else {
            throw new RuntimeException(context +
                    " must implement RemoteKeyListListener");
        }
    }

    /**
     * Called when the fragment is no longer attached to its activity.  This
     * is called after {@link #onDestroy()}.
     */
    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
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
                R.layout.fragment_remotekey_list, container, false);
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

        View subView = view.findViewById(R.id.recycler_view_remotekey_item);
        if (subView instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) subView;

            // Set the adapter
            Context context = recyclerView.getContext();
            LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(context);
            recyclerView.setLayoutManager(mLinearLayoutManager);
            mRemoteKeyItemAdapter = new RemoteKeyItemAdapter(
                    new RemoteKeyItemAdapter.RemoteKeyItemAdapterListener() {
                        @Override
                        public void onItemPicked(@NonNull RemoteKeyItem remoteKeyItem) {
                            Log.d(TAG, "onItemPicked: " + remoteKeyItem);
                            showRemoteKey(remoteKeyItem);
                        }

                        @Override
                        public void onDeleteAttempt(int id) {
                            Log.d(TAG, "onDeleteAttempt: id=" + id);
                            deleteRemoteKey(id);
                        }

                        @Override
                        public void onError(@NonNull String description) {
                            mListener.onError(description);
                        }
                    }
            );
            recyclerView.setAdapter(mRemoteKeyItemAdapter);

            // Set divider between items.
            setDividerItemDecorations(recyclerView);
        }
        if (mGetAccessTokenFromSharedPreferences) {
            readSharedPreferences();
        }
        showProgressBar(true);
    }

    private void setDividerItemDecorations(RecyclerView recyclerView) {
        DividerItemDecoration did = new DividerItemDecoration(
                recyclerView.getContext(),
                new LinearLayoutManager(getActivity()).getOrientation());
        recyclerView.addItemDecoration(did);
    }

    public void setFilter(@NonNull HashSet<String> fingerprints) {
        mFingerprints = fingerprints;
    }

    public void addFilter(@NonNull String fingerprint) {
        if (mFingerprints != null) {
            mFingerprints.add(fingerprint);
        }
    }

    public void delFilter(@NonNull String fingerprint) {
        if (mFingerprints != null) {
            mFingerprints.remove(fingerprint);
        }
    }

    private void readSharedPreferences() {
        SharedPrefsAccessKey sharedPrefsAccessKey =
                new SharedPrefsAccessKey(mContext);
        mServerUrl = sharedPrefsAccessKey.readAccessTokenServerUrl();
        mAccount = sharedPrefsAccessKey.readAccessTokenAccount();
        mSecretKey = sharedPrefsAccessKey.readAccessTokenSecretKey();

        if (mServerUrl != null && mAccount != null && mSecretKey != null) {
            setRemoteConfig(mServerUrl, mAccount, mSecretKey);
        } else {
            mListener.onError(TAG + ": Cannot read SharedPreference");
        }
    }

    public void setRemoteConfig(
            @NonNull String serverUrl,
            @NonNull String account,
            @NonNull String secretKey) {
        this.mServerUrl = serverUrl;
        this.mAccount = account;
        this.mSecretKey = secretKey;

        execPubKeyClient();
    }

    private void execPubKeyClient() {
        PubKeyClient pubKeyClient = new PubKeyClient(mContext,
                new PubKeyClient.PubKeyClientListener() {
                    @Override
                    public void onAccessToken(@NonNull String accessToken) {
                        Log.d(TAG, "onAccessToken: accessToken=" + accessToken);
                        mAccessToken = accessToken;
                        mPubKeyClient.getRemotePubKeyList(accessToken);
                    }

                    @Override
                    public void onRemotePubKey(@NonNull RemotePubKey remotePubKey) {
                        showProgressBar(false);
                        addRemotePubKey(remotePubKey);
                    }

                    @Override
                    public void onRemotePubKeys(@NonNull RemotePubKey[] remotePubKeys) {
                        showProgressBar(false);
                        addRemotePubKeys(remotePubKeys);
                    }

                    @Override
                    public void onError(@NonNull String description) {
                        /* Relay error info to the listener */
                        mListener.onError(description);
                    }
                });

        mPubKeyClient = pubKeyClient;
        pubKeyClient.getAccessToken(mServerUrl, mAccount, mSecretKey);
    }

    private boolean compareFingerprint(@NonNull String fingerprint) {
        return mFingerprints.contains(
                fingerprint.substring(FINGERPRINT_PREFIX.length()));
    }

    private void addRemotePubKey(@NonNull RemotePubKey remotePubKey) {
        int id = remotePubKey.getId();
        String fingerprint = remotePubKey.getFingerprint();
        String comment = remotePubKey.getComment();
        boolean isDefault = remotePubKey.isDefaultKey();
        String createdAt = remotePubKey.getCreatedAt();

        if (compareFingerprint(fingerprint)) {
            mRemoteKeyItemAdapter.addItem(id, fingerprint, comment, isDefault, createdAt);
        } else {
            Log.w(TAG, "Unknown fingerprint, skip: " + remotePubKey);
        }
        showEmptyMessage(mRemoteKeyItemAdapter.getItemCount() <= 0);
    }

    private void addRemotePubKeys(@NonNull RemotePubKey[] remotePubKeys) {
        for (RemotePubKey remotePubKey : remotePubKeys) {
            int id = remotePubKey.getId();
            String fingerprint = remotePubKey.getFingerprint();
            String comment = remotePubKey.getComment();
            boolean isDefault = remotePubKey.isDefaultKey();
            String createdAt = remotePubKey.getCreatedAt();

            if (compareFingerprint(fingerprint)) {
                mRemoteKeyItemAdapter.addItem(id, fingerprint, comment, isDefault, createdAt);
            } else {
                Log.w(TAG, "Unknown fingerprint, skip: " + remotePubKey);
            }
        }
        showEmptyMessage(mRemoteKeyItemAdapter.getItemCount() <= 0);
    }

    private void delRemoteKeyItem(int id) {
        mPubKeyClient.delRemotePubKey(id, mAccessToken);

        mRemoteKeyItemAdapter.delItem(id);
        showEmptyMessage(mRemoteKeyItemAdapter.getItemCount() <= 0);
    }

    public void registerPublicKey(@NonNull String pem,
                                  @Nullable String comment,
                                  boolean isDefaultKey) {
        mPubKeyClient.addRemotePubKey(pem,
                comment,
                isDefaultKey,
                mAccessToken);
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

    private void showEmptyMessage(boolean isEmpty) {
        View rootView = getView();
        if (rootView != null) {
            RecyclerView recyclerView =
                    rootView.findViewById(R.id.recycler_view_remotekey_item);
            TextView emptyView =
                    rootView.findViewById(R.id.textview_empty_remotekey);

            if (isEmpty) {
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.GONE);
                }
                if (emptyView != null) {
                    emptyView.setVisibility(View.VISIBLE);
                }
            } else {
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                }
                if (emptyView != null) {
                    emptyView.setVisibility(View.GONE);
                }
            }
        } else {
            String message = TAG + ": showEmptyMessage: RootView not found?";
            mListener.onError(message);
        }
    }

    private void showRemoteKey(@NonNull RemoteKeyItem remoteKeyItem) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            InfoDialogFragment idf = new InfoDialogFragment(
                    fragmentActivity,
                    new InfoDialogFragment.InfoDialogFragmentListener() {
                        @Override
                        public void onPositiveButtonClicked() {
                            /* Do nothing, for now... */
                        }
                    });

            Bundle bundle = new Bundle();
            bundle.putString(BundleKeys.BUNDLE_KEY_INFO_MESSAGE, remoteKeyItem.toString());
            idf.setArguments(bundle);

            try {
                idf.show(fragmentActivity.getSupportFragmentManager(),
                        InfoDialogFragment.class.getSimpleName());
            } catch (IllegalStateException e) {
                mListener.onError(TAG + ": InfoDialogFragment: " + e.getMessage());
            }
        }
    }

    public boolean hasAlreadyRegistered(@NonNull String fingerprint) {
        return (mRemoteKeyItemAdapter.lookupRemoteKeyId(
                FINGERPRINT_PREFIX + fingerprint) >= 0);
    }

    public void deleteRemoteKeyByFingerprint(@NonNull String fingerprint) {
        int id = mRemoteKeyItemAdapter.lookupRemoteKeyId(
                FINGERPRINT_PREFIX + fingerprint);
        if (id >= 0) {
            deleteRemoteKey(id);
        } else {
            mListener.onWarning("RemoteKey not found: fingerprint=" + fingerprint);
        }
    }

    private void deleteRemoteKey(int id) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            ConfirmDialogFragment cdf = new ConfirmDialogFragment(
                    fragmentActivity,
                    new ConfirmDialogFragment.ConfirmDialogFragmentListener() {
                        @Override
                        public void onPositiveButtonClicked() {
                            delRemoteKeyItem(id);
                        }

                        @Override
                        public void onNegativeButtonClicked() {
                            /* Operation canceled */
                        }
                    });

            Bundle bundle = new Bundle();
            bundle.putString(BundleKeys.BUNDLE_KEY_CONFIRM_MESSAGE,
                    "Delete PublicKey (id: " + id + ") from the configuration server?");
            cdf.setArguments(bundle);

            try {
                cdf.show(fragmentActivity.getSupportFragmentManager(),
                        ConfirmDialogFragment.class.getSimpleName());
            } catch (IllegalStateException e) {
                mListener.onError(TAG + ": ConfirmDialogFragment: " + e.getMessage());
            }
        }
    }

    public interface RemoteKeyListListener {
        void onWarning(@NonNull String description);
        void onError(@NonNull String description);
    }
}
