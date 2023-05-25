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

package com.example.samplepublisher;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.samplepublisher.constants.BundleKeys;
import com.example.samplepublisher.ui.dialogs.ErrorDialogFragment;
import com.example.samplepublisher.ui.keypair.KeyPairListFragment;
import com.example.samplepublisher.ui.keypair.RemoteKeyListFragment;

import java.util.HashSet;

public class KeyManageActivity extends AppCompatActivity implements
        KeyPairListFragment.KeyPairListListener,
        RemoteKeyListFragment.RemoteKeyListListener,
        ErrorDialogFragment.ErrorDialogListener {
    private final String TAG = KeyManageActivity.class.getSimpleName();

    /**
     * {@inheritDoc}
     * <p>
     * Perform initialization of all fragments.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_manage);

        if (savedInstanceState != null) {
            /* Avoid creating the same fragment sets more than once. */
            Log.d(TAG, "onCreate: After RESTART");
        } else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            RemoteKeyListFragment remoteKeyListFragment = new RemoteKeyListFragment();
            transaction.add(R.id.fragmentContainerView_remotekey, remoteKeyListFragment,
                    RemoteKeyListFragment.class.getSimpleName());

            KeyPairListFragment keyPairListFragment = new KeyPairListFragment();
            transaction.add(R.id.fragmentContainerView_keypair, keyPairListFragment,
                    KeyPairListFragment.class.getSimpleName());

            transaction.commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.activity_name_settings);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true); // onOptionsItemSelected
        }
    }

    /*
     * KeyPair stuff
     */
    @Override
    public void onKeyPairFingerprints(@NonNull HashSet<String> fingerprints) {
        /* Implementation of KeyPairListFragment.KeyPairListListener */
        RemoteKeyListFragment remoteKeyListFragment = lookupRemoteKeyListFragment();
        if (remoteKeyListFragment != null) {
            /* Keep initial set of fingerprints in the AndroidKeyStore */
            remoteKeyListFragment.setFilter(fingerprints);
        }
    }

    @Override
    public void onKeyPairAdded(@NonNull String fingerprint) {
        /* Implementation of KeyPairListFragment.KeyPairListListener */
        RemoteKeyListFragment remoteKeyListFragment = lookupRemoteKeyListFragment();
        if (remoteKeyListFragment != null) {
            /* New keypair has added to the AndroidKeyStore */
            remoteKeyListFragment.addFilter(fingerprint);
        }
    }

    @Override
    public void onKeyPairDeleted(@NonNull String fingerprint) {
        /* Implementation of KeyPairListFragment.KeyPairListListener */
        RemoteKeyListFragment remoteKeyListFragment = lookupRemoteKeyListFragment();
        if (remoteKeyListFragment != null) {
            /* A Keypair has deleted from the AndroidKeyStore */
            remoteKeyListFragment.delFilter(fingerprint);
            /* Delete the public key which has registered to the configuration server */
            remoteKeyListFragment.deleteRemoteKeyByFingerprint(fingerprint);
        }
    }

    @Override
    public void onSubmitPublicKey(@NonNull String fingerprint,
                                  @NonNull String pem,
                                  @Nullable String comment) {
        /* Implementation of KeyPairListFragment.KeyPairListListener */
        RemoteKeyListFragment remoteKeyListFragment = lookupRemoteKeyListFragment();
        if (remoteKeyListFragment != null) {
            /* Avoid registering the identical key multiple times */
            if (remoteKeyListFragment.hasAlreadyRegistered(fingerprint)) {
                onWarning("RemoteKey already exists:\n" +
                        "fingerprint=" + fingerprint);
                return;
            }

            /* OK, go ahead */
            remoteKeyListFragment.registerPublicKey(pem, comment, true);
        }
    }

    /*
     * RemoteKeyList stuff
     */
    @Nullable
    private RemoteKeyListFragment lookupRemoteKeyListFragment() {
        RemoteKeyListFragment fragment;
        fragment = (RemoteKeyListFragment) getSupportFragmentManager().
                findFragmentByTag(RemoteKeyListFragment.class.getSimpleName());
        if (fragment == null) {
            Log.e(TAG, "RemoteKeyListFragment not found?");
        }
        return fragment;
    }

    @Override
    public void onWarning(@NonNull String description) {
        /* Implementation of RemoteKeyListFragment.RemoteKeyListListener */
        showErrorDialog(description, false);
    }

    @Override
    public void onError(@NonNull String description) {
        /* Implementation of KeyPairListFragment.KeyPairListListener */
        /* Implementation of RemoteKeyListFragment.RemoteKeyListListener */
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
