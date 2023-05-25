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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplepublisher.R;
import com.example.samplepublisher.models.KeyPairItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class KeyPairItemAdapter
        extends RecyclerView.Adapter<KeyPairItemAdapter.ViewHolder> {
    private final String TAG = KeyPairItemAdapter.class.getSimpleName();

    private final KeyPairItemAdapterListener mListener;

    /* Build an empty KeyPairItemList. It's contents will be dynamically updated. */
    private final List<KeyPairItem> mKeyPairItemList = new ArrayList<>();
    /* Use a HashMap to keep ViewHolder by corresponding KeyAlias */
    private final HashMap<String, ViewHolder> mViewHolderMap = new HashMap<>();

    public KeyPairItemAdapter(@NonNull KeyPairItemAdapterListener listener) {
        this.mListener = listener;
    }

    /**
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     * <p>
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     * <p>
     * The new ViewHolder will be used to display items of the adapter using
     * {@link #onBindViewHolder(ViewHolder, int, List)}. Since it will be re-used to display
     * different items in the data set, it is a good idea to cache references to sub views of
     * the View to avoid unnecessary {@link View#findViewById(int)} calls.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     * @see #getItemViewType(int)
     * @see #onBindViewHolder(ViewHolder, int)
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.keypair_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
     * position.
     * <p>
     * Note that unlike {@link ListView}, RecyclerView will not call this method
     * again if the position of the item changes in the data set unless the item itself is
     * invalidated or the new position cannot be determined. For this reason, you should only
     * use the <code>position</code> parameter while acquiring the related data item inside
     * this method and should not keep a copy of it. If you need the position of an item later
     * on (e.g. in a click listener), use {@link ViewHolder#getAdapterPosition()} which will
     * have the updated adapter position.
     * <p>
     * Override {@link #onBindViewHolder(ViewHolder, int, List)} instead if Adapter can
     * handle efficient partial bind.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        KeyPairItem keyPairItem;
        try {
            keyPairItem = mKeyPairItemList.get(position);
        } catch (IndexOutOfBoundsException e) {
            mListener.onError(TAG + ": KeyPairItemList" +
                    ".get(" + position + "): " + e.getMessage());
            return;
        }

        int id = keyPairItem.getId();
        holder.mIdView.setText(String.format(Locale.US,"#%d", id));

        String keyAlias = keyPairItem.getKeyAlias();
        holder.mAliasView.setText(keyAlias);

        /* Keep this holder with the corresponding KeyAlias */
        try {
            mViewHolderMap.put(keyAlias, holder);
        } catch (UnsupportedOperationException |
                ClassCastException |
                NullPointerException |
                IllegalArgumentException e) {
            mListener.onError(TAG + ": ViewHolderMap" +
                    ".put(" + keyAlias + "): " + e.getMessage());
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return mKeyPairItemList.size();
    }

    public void addItem(@NonNull String keyAlias,
                        @NonNull String fingerprint) {
        /*
         * It seems that the AndroidKeyStore creates new KeyPair
         * regardless the corresponding alias duplication.
         * We check the uniqueness using a HashMap.
         */
        if (mViewHolderMap.containsKey(keyAlias)) {
            mListener.onWarning("Alias (" + keyAlias + ") already exists");
        } else {
            int id = getItemCount() + 1;
            mKeyPairItemList.add(new KeyPairItem(id, keyAlias, fingerprint));
            notifyItemInserted(mKeyPairItemList.size() - 1);
        }
    }

    @Nullable
    public KeyPairItem getItem(@NonNull String keyAlias) {
        KeyPairItem item = null;
        if (mViewHolderMap.containsKey(keyAlias)) {
            ViewHolder holder;
            try {
                holder = mViewHolderMap.get(keyAlias);
            } catch (UnsupportedOperationException |
                    ClassCastException |
                    NullPointerException |
                    IllegalArgumentException e) {
                mListener.onError(TAG + ": ViewHolderMap" +
                        ".get(" + keyAlias + "): " + e.getMessage());
                return null;
            }
            if (holder != null) {
                int position = holder.getBindingAdapterPosition();
                try {
                    item = mKeyPairItemList.get(position);
                } catch (IndexOutOfBoundsException e) {
                    mListener.onError(TAG + ": KeyPairItemList" +
                            ".get(" + position + "): " + e.getMessage());
                    return null;
                }
            }
        }
        return item;
    }

    public void delItem(@NonNull String keyAlias) {
        if (mViewHolderMap.containsKey(keyAlias)) {
            ViewHolder holder;
            try {
                holder = mViewHolderMap.get(keyAlias);
            } catch (UnsupportedOperationException |
                    ClassCastException |
                    NullPointerException |
                    IllegalArgumentException e) {
                mListener.onError(TAG + ": ViewHolderMap" +
                        ".get(" + keyAlias + "): " + e.getMessage());
                return;
            }
            if (holder != null) {
                /*
                 * To use "ViewHolder.getBindingAdapterPosition()",
                 * add following line in build.gradle in app level.
                 * "implementation 'androidx.recyclerview:recyclerview:1.2.1'"
                 */
                int position = holder.getBindingAdapterPosition();
                try {
                    mKeyPairItemList.remove(position);
                    notifyItemRemoved(position);
                } catch (UnsupportedOperationException | IndexOutOfBoundsException e) {
                    Log.e(TAG, "KeyPairItemList.remove(): " + e.getMessage());
                    /* Go ahead */
                }

                try {
                    mViewHolderMap.remove(keyAlias);
                } catch (UnsupportedOperationException |
                        ClassCastException |
                        NullPointerException e) {
                    Log.e(TAG, "ViewHolderMap.remote(): " + e.getMessage());
                    /* Go ahead */
                }
            }
        } else {
            mListener.onWarning("Unknown alias(" + keyAlias + "), do nothing");
        }
    }

    @NonNull
    public HashSet<String> getFingerprints() {
        HashSet<String> fingerprints = new HashSet<>();
        for (int i = 0; i < mKeyPairItemList.size(); i++) {
            KeyPairItem keyPairItem = mKeyPairItemList.get(i);
            fingerprints.add(keyPairItem.getFingerprint());
        }
        return fingerprints;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView mIdView;
        final TextView mAliasView;
        final ImageButton mSendView;
        final ImageButton mDeleteView;

        ViewHolder(View view) {
            super(view);

            mIdView = view.findViewById(R.id.textview_keypair_item_number);

            mAliasView = view.findViewById(R.id.textview_keypair_alias);
            mAliasView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView tv = (TextView) v;
                    String keyAlias = tv.getText().toString();
                    mListener.onInfo(keyAlias);
                }
            });

            mSendView = view.findViewById(R.id.button_submit_keypair);
            mSendView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String keyAlias = mAliasView.getText().toString();
                    mListener.onPickToSend(keyAlias);
                }
            });

            mDeleteView = view.findViewById(R.id.button_delete_keypair);
            mDeleteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String keyAlias = mAliasView.getText().toString();
                    mListener.onDelete(keyAlias);
                }
            });
        }
    }

    public interface KeyPairItemAdapterListener {
        void onPickToSend(@NonNull String alias);
        void onInfo(@NonNull String alias);
        void onDelete(@NonNull String alias);
        void onWarning(@NonNull String description);
        void onError(@NonNull String description);
    }
}
