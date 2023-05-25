/*
 * Copyright (c) 2022 National Institute of Informatics
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

package com.example.samplepublisher.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.samplepublisher.R;
import com.example.samplepublisher.constants.BundleKeys;

public class InfoDialogFragment extends DialogFragment {
    private final String TAG = InfoDialogFragment.class.getSimpleName();

    private final Context mContext;
    private final InfoDialogFragmentListener mListener;
    private String mDialogMessage;

    public InfoDialogFragment(@NonNull Context context,
                              @NonNull InfoDialogFragmentListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            mDialogMessage = bundle.getString(BundleKeys.BUNDLE_KEY_INFO_MESSAGE);
            if (mDialogMessage == null) {
                Log.w(TAG, "Bundle does not have the dialog message?");
            }
        } else {
            Log.w(TAG, "Bundle is missing?");
        }
        if (mDialogMessage == null) {
            mDialogMessage = "N/A";
        }
    }

    /**
     * Override to build your own custom Dialog container.  This is typically
     * used to show an AlertDialog instead of a generic Dialog; when doing so,
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} does not need
     * to be implemented since the AlertDialog takes care of its own content.
     *
     * <p>This method will be called after {@link #onCreate(Bundle)} and
     * immediately before {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.  The
     * default implementation simply instantiates and returns a {@link Dialog}
     * class.
     *
     * <p><em>Note: DialogFragment own the {@link Dialog#setOnCancelListener
     * Dialog.setOnCancelListener} and {@link Dialog#setOnDismissListener
     * Dialog.setOnDismissListener} callbacks.  You must not set them yourself.</em>
     * To find out about these events, override {@link #onCancel(DialogInterface)}
     * and {@link #onDismiss(DialogInterface)}.</p>
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     *                           or null if this is a freshly created Fragment.
     * @return Return a new Dialog instance to be displayed by the Fragment.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // return super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder =
                new AlertDialog.Builder(mContext);

        builder.setTitle(getString(R.string.dialog_title_info));
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setMessage(mDialogMessage);
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onPositiveButtonClicked();
                        dialog.dismiss();
                    }
                });

        // Create an AlertDialog object and return it
        Dialog alertDialog = builder.create();

        // Make this dialog cancelable, but don't allow user to cancel by touching
        // outside of the dialog.
        alertDialog.setCancelable(true);
        alertDialog.setCanceledOnTouchOutside(false);

        return alertDialog;
    }

    public interface InfoDialogFragmentListener {
        void onPositiveButtonClicked();
    }
}
