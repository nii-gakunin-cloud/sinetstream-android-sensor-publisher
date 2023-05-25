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
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplepublisher.R;
import com.example.samplepublisher.models.RemoteKeyItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RemoteKeyItemAdapter
        extends RecyclerView.Adapter<RemoteKeyItemAdapter.ViewHolder> {
    private final String TAG = RemoteKeyItemAdapter.class.getSimpleName();

    private final RemoteKeyItemAdapterListener mListener;

    /* Build an empty RemoteKeyItemList. It's contents will be dynamically updated. */
    private final List<RemoteKeyItem> mRemoteKeyItemList = new ArrayList<>();
    /* Use a HashMap to keep ViewHolder by corresponding id */
    private final HashMap<Integer, ViewHolder> mViewHolderIdMap = new HashMap<>();
    /* Use a HashMap to keep id by corresponding fingerprint */
    private final HashMap<String, Integer> mFingerprintIdMap = new HashMap<>();

    public RemoteKeyItemAdapter(
            @NonNull RemoteKeyItemAdapterListener listener) {
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
                .inflate(R.layout.remotekey_item, parent, false);
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
     * on (e.g. in a click listener), use {@link ViewHolder#getBindingAdapterPosition()} which
     * will have the updated adapter position.
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
        RemoteKeyItem remoteKeyItem = mRemoteKeyItemList.get(position);

        int id = remoteKeyItem.getId();
        holder.mIdView.setText(String.format(Locale.US, "%d", id));
        String fingerprint = remoteKeyItem.getFingerprint();
        //holder.mCheckBox.setText(fingerprint);
        holder.mFingerPrintView.setText(fingerprint);
        boolean isDefault = remoteKeyItem.getIsDefault();
        holder.mCheckBox.setChecked(isDefault);
        holder.mCheckBox.setClickable(false);

        /* Keep this holder with the corresponding id */
        mViewHolderIdMap.put(id, holder);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return mRemoteKeyItemList.size();
    }

    public void addItem(int id,
                        @NonNull String fingerprint,
                        @NonNull String comment,
                        boolean isDefault,
                        @NonNull String createdAt) {
        /* Each ID cannot be duplicated, while identical fingerprints can coexist. */
        if (mViewHolderIdMap.containsKey(id)) {
            Log.w(TAG, "Going to update item: id (" + id + ")");
            ViewHolder holder = mViewHolderIdMap.get(id);
            if (holder != null) {
                int position = holder.getBindingAdapterPosition();
                RemoteKeyItem elem =
                        mRemoteKeyItemList.get(position);
                if (elem != null) {
                    if (elem.getIsDefault() != isDefault) {
                        Log.d(TAG, "id(" + id + "): isDefault: " +
                                elem.getIsDefault() + " -> " + isDefault);
                        elem.setIsDefault(isDefault);
                        mRemoteKeyItemList.set(position, elem);
                        notifyItemChanged(position);
                    }
                }
            }
        } else {
            mRemoteKeyItemList.add(
                    new RemoteKeyItem(id, fingerprint, comment, isDefault, createdAt));
            notifyItemInserted(mRemoteKeyItemList.size() - 1);
            mFingerprintIdMap.put(fingerprint, id);
        }
    }

    public int lookupRemoteKeyId(@NonNull String fingerprint) {
        try {
            Integer id = mFingerprintIdMap.get(fingerprint);
            return (id != null ? id : -1);
        } catch (ClassCastException | NullPointerException e) {
            Log.w(TAG, "FingerprintIdMap: fingerprint(" + fingerprint + ")" + e.getMessage());
            return -1;
        }
    }

    public void delItem(int id) {
        if (mViewHolderIdMap.containsKey(id)) {
            ViewHolder holder = mViewHolderIdMap.get(id);
            if (holder != null) {
                int position = holder.getBindingAdapterPosition();
                RemoteKeyItem item = mRemoteKeyItemList.get(position);
                mFingerprintIdMap.remove(item.getFingerprint());
                mRemoteKeyItemList.remove(position);
                notifyItemRemoved(position);

                mViewHolderIdMap.remove(id);
            }
        } else {
            Log.w(TAG, "delItem: Unknown id(" + id + ")");
        }
    }

    @Nullable
    public RemoteKeyItem lookupRemoteKeyItem(int id) {
        if (mViewHolderIdMap.containsKey(id)) {
            ViewHolder holder = mViewHolderIdMap.get(id);
            if (holder != null) {
                int position = holder.getBindingAdapterPosition();
                return mRemoteKeyItemList.get(position);
            }
        } else {
            Log.w(TAG, "getInfo: Unknown id (" + id + ")");
        }
        return null;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView mIdView;
        final CheckBox mCheckBox;
        final TextView mFingerPrintView;
        final ImageButton mDeleteView;

        ViewHolder(View view) {
            super(view);

            mIdView = view.findViewById(R.id.textview_remotekey_id);
            mCheckBox = view.findViewById(R.id.checkbox_isdefault);
            mFingerPrintView = view.findViewById(R.id.textview_remotekey_fingerprint);
            mFingerPrintView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String idStr = mIdView.getText().toString();
                    try {
                        int id = Integer.parseInt(idStr);
                        RemoteKeyItem remoteKeyItem = lookupRemoteKeyItem(id);
                        if (remoteKeyItem != null) {
                            mListener.onItemPicked(remoteKeyItem);
                        }
                    } catch (NumberFormatException e) {
                        mListener.onError(TAG + ": DeleteItem: " + e.getMessage());
                    }
                }
            });

            mDeleteView = view.findViewById(R.id.button_delete_remotekey);
            mDeleteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String idStr = mIdView.getText().toString();
                    try {
                        int id = Integer.parseInt(idStr);
                        mListener.onDeleteAttempt(id);
                    } catch (NumberFormatException e) {
                        mListener.onError(TAG + ": DeleteItem: " + e.getMessage());
                    }
                }
            });
        }
    }

    public interface RemoteKeyItemAdapterListener {
        void onItemPicked(@NonNull RemoteKeyItem remoteKeyItem);
        void onDeleteAttempt(int id);
        void onError(@NonNull String description);
    }
}
