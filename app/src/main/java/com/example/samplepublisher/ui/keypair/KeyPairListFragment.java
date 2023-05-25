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
import com.example.samplepublisher.models.KeyPairItem;
import com.example.samplepublisher.ui.dialogs.ConfirmDialogFragment;
import com.example.samplepublisher.ui.dialogs.InfoDialogFragment;
import com.example.samplepublisher.ui.dialogs.InputDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashSet;

import jp.ad.sinet.stream.android.config.remote.keystore.KeyPairHandler;

public class KeyPairListFragment extends Fragment {
    private final String TAG = KeyPairListFragment.class.getSimpleName();

    private KeyPairItemAdapter mKeyPairItemAdapter;
    private KeyPairListListener mListener = null;
    private KeyPairHandler mKeyPairHandler = null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public static KeyPairListFragment newInstance() {
        return new KeyPairListFragment();
    }

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     *
     * @param context The Context which implements KeyPairListListener
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof KeyPairListListener) {
            mListener = (KeyPairListListener) context;
        } else {
            throw new RuntimeException(context +
                    " must implement KeyPairListListener");
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
        Log.d(TAG, "onCreateView");
        //return super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(
                R.layout.fragment_keypair_list, container, false);
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
        Log.d(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        View subView = view.findViewById(R.id.recyclerview_keypair_item);
        if (subView instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) subView;

            // Set the adapter
            Context context = recyclerView.getContext();
            LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(context);
            recyclerView.setLayoutManager(mLinearLayoutManager);
            mKeyPairItemAdapter = new KeyPairItemAdapter(
                    new KeyPairItemAdapter.KeyPairItemAdapterListener() {
                        @Override
                        public void onPickToSend(@NonNull String alias) {
                            submitPublicKey(alias);
                        }

                        @Override
                        public void onInfo(@NonNull String alias) {
                            showKeyPair(alias);
                        }

                        @Override
                        public void onDelete(@NonNull String alias) {
                            deleteKeyPair(alias);
                        }

                        @Override
                        public void onWarning(@NonNull String description) {
                            mListener.onWarning(description);
                        }

                        @Override
                        public void onError(@NonNull String description) {
                            mListener.onError(description);
                        }
                    }
            );
            recyclerView.setAdapter(mKeyPairItemAdapter);

            // Set divider between items.
            setDividerItemDecorations(recyclerView);
        } else {
            Log.e(TAG, "RecyclerView not found?");
        }

        subView = view.findViewById(R.id.fab_keypair);
        if (subView instanceof FloatingActionButton) {
            FloatingActionButton fab = (FloatingActionButton) subView;
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setFabClickListener();
                }
            });
        } else {
            Log.e(TAG, "FloatingActionButton not found?");
        }
    }

    /**
     * Called when all saved state has been restored into the view hierarchy
     * of the fragment.  This can be used to do initialization based on saved
     * state that you are letting the view hierarchy track itself, such as
     * whether check box widgets are currently checked.  This is called
     * after {@link #onViewCreated(View, Bundle)} and before {@link #onStart()}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewStateRestored");
        super.onViewStateRestored(savedInstanceState);
        setKeyPairHandler();
    }

    private void setDividerItemDecorations(RecyclerView recyclerView) {
        DividerItemDecoration did = new DividerItemDecoration(
                recyclerView.getContext(),
                new LinearLayoutManager(getActivity()).getOrientation());
        recyclerView.addItemDecoration(did);
    }

    private void setKeyAliases(@NonNull String[] keyAliases) {
        for (String keyAlias : keyAliases) {
            String fingerprint = mKeyPairHandler.calcFingerprint(keyAlias);
            if (fingerprint == null) {
                /* mKeyPairHandler.onError() must have called */
                return;
            }
            mKeyPairItemAdapter.addItem(keyAlias, fingerprint);
        }
    }

    private void addKeyAlias(@NonNull String keyAlias) {
        String fingerprint = mKeyPairHandler.calcFingerprint(keyAlias);
        if (fingerprint == null) {
            /* mKeyPairHandler.onError() must have called */
            return;
        }
        mKeyPairItemAdapter.addItem(keyAlias, fingerprint);
        mListener.onKeyPairAdded(fingerprint);
        showEmptyMessage(false);
    }

    private void delKeyAlias(@NonNull String keyAlias) {
        String fingerprint = null;
        KeyPairItem item = mKeyPairItemAdapter.getItem(keyAlias);
        if (item != null) {
            fingerprint = item.getFingerprint();
        }

        mKeyPairItemAdapter.delItem(keyAlias);
        if (fingerprint != null) {
            mListener.onKeyPairDeleted(fingerprint);
        }
        showEmptyMessage(mKeyPairItemAdapter.getItemCount() <= 0);
    }

    private void showEmptyMessage(boolean isEmpty) {
        View rootView = getView();
        if (rootView != null) {
            RecyclerView recyclerView =
                    rootView.findViewById(R.id.recyclerview_keypair_item);
            TextView emptyView =
                    rootView.findViewById(R.id.textview_empty_keypair);

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

    private void setKeyPairHandler() {
        mKeyPairHandler = new KeyPairHandler(
                new KeyPairHandler.KeyPairHandlerListener() {
                    @Override
                    public void onAliasNames(@NonNull String[] aliases) {
                        if (aliases.length > 0) {
                            showEmptyMessage(false);
                            setKeyAliases(aliases);
                        } else {
                            showEmptyMessage(true);
                        }

                        mListener.onKeyPairFingerprints(
                                mKeyPairItemAdapter.getFingerprints());
                    }

                    @Override
                    public void onPublicKey(
                            @NonNull String alias, @NonNull String base64String) {
                        Log.d(TAG, "onPublicKey: alias(" + alias + ")\n" +
                                "PublicKey:\n" + base64String);
                        addKeyAlias(alias);
                    }

                    @Override
                    public void onError(@NonNull String description) {
                        mListener.onError(description);
                    }
                });

        mKeyPairHandler.listAliases();
    }

    private void setFabClickListener() {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            InputDialogFragment idf = new InputDialogFragment(
                    fragmentActivity,
                    new InputDialogFragment.InputDialogFragmentListener() {
                        @Override
                        public void onPositiveButtonClicked(@NonNull String stringValue) {
                            Log.d(TAG, "onPositiveButtonClicked: InputText(" + stringValue + ")");
                            mKeyPairHandler.createOrReusePublicKey(stringValue);
                        }

                        @Override
                        public void onError(@NonNull String description) {
                            mListener.onError(description);
                        }
                    });
            try {
                idf.show(fragmentActivity.getSupportFragmentManager(),
                        InputDialogFragment.class.getSimpleName());
            } catch (IllegalStateException e) {
                mListener.onError(TAG + ": InputDialogFragment: " + e.getMessage());
            }
        }
    }

    private void showKeyPair(@NonNull String alias) {
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

            String info = mKeyPairHandler.getPublicKeyInfo(alias);
            if (info != null) {
                Bundle bundle = new Bundle();
                bundle.putString(BundleKeys.BUNDLE_KEY_INFO_MESSAGE, info);
                idf.setArguments(bundle);
            }

            try {
                idf.show(fragmentActivity.getSupportFragmentManager(),
                        InfoDialogFragment.class.getSimpleName());
            } catch (IllegalStateException e) {
                mListener.onError(TAG + ": InfoDialogFragment: " + e.getMessage());
            }
        }
    }

    private void deleteKeyPair(@NonNull String alias) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            ConfirmDialogFragment cdf = new ConfirmDialogFragment(
                    fragmentActivity,
                    new ConfirmDialogFragment.ConfirmDialogFragmentListener() {
                        @Override
                        public void onPositiveButtonClicked() {
                            mKeyPairHandler.deleteKeyPair(alias);
                            delKeyAlias(alias);
                        }

                        @Override
                        public void onNegativeButtonClicked() {
                            Log.d(TAG, "Delete KeyPair canceled");
                        }
                    });

            Bundle bundle = new Bundle();
            bundle.putString(BundleKeys.BUNDLE_KEY_CONFIRM_MESSAGE,
                    "Delete KeyPair (alias: " + alias + ") from the Android KeyStore?");
            cdf.setArguments(bundle);

            try {
                cdf.show(fragmentActivity.getSupportFragmentManager(),
                        ConfirmDialogFragment.class.getSimpleName());
            } catch (IllegalStateException e) {
                mListener.onError(TAG + ": ConfirmDialogFragment: " + e.getMessage());
            }
        }
    }

    private void submitPublicKey(@NonNull String alias) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity != null) {
            KeyPairItem keyPairItem = mKeyPairItemAdapter.getItem(alias);
            if (keyPairItem == null) {
                mListener.onError("KeyPairItem not found: alias=" + alias);
                return;
            }
            ConfirmDialogFragment cdf = new ConfirmDialogFragment(
                    fragmentActivity,
                    new ConfirmDialogFragment.ConfirmDialogFragmentListener() {
                        @Override
                        public void onPositiveButtonClicked() {
                            /*
                             * Convert the public key to a PEM encoded string (RFC5280).
                             */
                            String base64EncodedPublicKey =
                                    mKeyPairHandler.getBase64EncodedPublicKey(alias);
                            if (base64EncodedPublicKey != null) {
                                String pem = "";
                                pem += "-----BEGIN PUBLIC KEY-----\n";
                                pem += base64EncodedPublicKey + "\n";
                                pem += "-----END PUBLIC KEY-----";

                                String fingerprint = keyPairItem.getFingerprint();
                                mListener.onSubmitPublicKey(fingerprint, pem, alias);
                            }
                        }

                        @Override
                        public void onNegativeButtonClicked() {
                            /* Operation canceled */
                        }
                    });

            Bundle bundle = new Bundle();
            bundle.putString(BundleKeys.BUNDLE_KEY_CONFIRM_MESSAGE,
                    "Register this PublicKey to the configuration server?");
            cdf.setArguments(bundle);

            try {
                cdf.show(fragmentActivity.getSupportFragmentManager(),
                        ConfirmDialogFragment.class.getSimpleName());
            } catch (IllegalStateException e) {
                mListener.onError(TAG + ": ConfirmDialogFragment: " + e.getMessage());
            }
        }
    }

    public interface KeyPairListListener {
        void onKeyPairFingerprints(@NonNull HashSet<String> fingerprints);
        void onKeyPairAdded(@NonNull String fingerprint);
        void onKeyPairDeleted(@NonNull String fingerprint);
        void onSubmitPublicKey(@NonNull String fingerprint,
                               @NonNull String pem,
                               @Nullable String comment);
        void onWarning(@NonNull String description);
        void onError(@NonNull String description);
    }
}
