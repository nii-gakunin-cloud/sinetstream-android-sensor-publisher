package com.example.samplepublisher.ui.main;

import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplepublisher.R;
import com.example.samplepublisher.models.SensorItem;

import java.util.ArrayList;
import java.util.List;

public class SensorItemAdapter extends
        RecyclerView.Adapter<SensorItemAdapter.ViewHolder> {
    private final static String TAG = SensorItemAdapter.class.getSimpleName();

    private final List<SensorItem> mSensorItemList;
    private final SparseArray<SensorItemAdapter.ViewHolder> mViewHolderMap;
    private final SensorItemAdapterListener mListener;
    private boolean mIsCollectiveOperation = false;

    public SensorItemAdapter(@NonNull List<SensorItem> sensorItemList,
                             @NonNull SensorItemAdapterListener mListener) {
        this.mSensorItemList = sensorItemList;
        this.mViewHolderMap = new SparseArray<>();
        this.mListener = mListener;
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
    public SensorItemAdapter.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sensor_item, parent, false);
        return new SensorItemAdapter.ViewHolder(view);
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
    public void onBindViewHolder(@NonNull SensorItemAdapter.ViewHolder holder, int position) {
        SensorItem sensorItem;
        try {
            sensorItem = mSensorItemList.get(position);
        } catch (IndexOutOfBoundsException e) {
            mListener.onError(TAG + ": onBindViewHolder: " + e.getMessage());
            return;
        }

        /*
         * Here is a tricky part. Since RecyclerView recycles its rows
         * as user scrolls up/down, current "OnCheckedChangeListener"
         * attached to the old item might trigger unwanted situations.
         * We need to remove existing lister at first.
         *
         * https://stackoverflow.com/questions/32427889/checkbox-in-recyclerview-keeps-on-checking-different-items
         */
        holder.mCheckBox.setOnCheckedChangeListener(null);

        /* Set the holder contents from the corresponding SensorItem */
        String idx = "#" + (position + 1);
        holder.mIdView.setText(idx);
        holder.mCheckBox.setText(sensorItem.getSensorName());
        holder.mCheckBox.setChecked(sensorItem.getChecked());

        /* Keep this ViewHolder by sensorType */
        if (mViewHolderMap.get(sensorItem.getSensorType()) == null) {
            mViewHolderMap.put(sensorItem.getSensorType(), holder);
        }

        holder.mCheckBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(
                    CompoundButton compoundButton, boolean checked) {
                /*
                 * [NB]
                 * This method is called for rows within the RecyclerView scope,
                 * let's say it's around 10 items of the list.
                 */
                Log.d(TAG, "onCheckedChanged(" + sensorItem.getSensorName() + "): " + checked);

                if (mIsCollectiveOperation) {
                    /* Skip further processing */
                    return;
                }

                /* Keep the CheckBox check state in the SensorItem object */
                sensorItem.setChecked(checked);

                /* Notify listener that a CheckBox check state has changed */
                mListener.onItemCheckStateChanged(getCheckedItemCount(), getItemCount());
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return mSensorItemList.size();
    }

    public int getCheckedItemCount() {
        int checkedItems = 0;
        for (int i = 0, n = getItemCount(); i < n; i++) {
            SensorItem sensorItem = mSensorItemList.get(i);
            if (sensorItem.getChecked()) {
                checkedItems++;
            }
        }
        return checkedItems;
    }

    public boolean isAnyItemChecked() {
        return (getCheckedItemCount() > 0);
    }

    public ArrayList<Integer> getCheckedSensorTypes() {
        ArrayList<Integer> sensorTypes = new ArrayList<>();

        for (int i = 0, n = mSensorItemList.size(); i < n; i++) {
            SensorItem sensorItem = mSensorItemList.get(i);
            if (sensorItem.getChecked()) {
                int sensorType = sensorItem.getSensorType();
                sensorTypes.add(sensorType);
            }
        }
        return sensorTypes;
    }

    public void checkAllSensorTypes(boolean checked) {
        mIsCollectiveOperation = true;
        for (int i = 0, n = mSensorItemList.size(); i < n; i++) {
            SensorItem sensorItem = mSensorItemList.get(i);

            /* Update the latest check state in the SensorItem object */
            sensorItem.setChecked(checked);

            /*
             * Reflect to the CheckBox in the RecyclerView
             *
             * [NB]
             * To avoid weird situation, just check/uncheck the CheckBox state
             * but suppress further processing within its OnCheckedChangeListener.
             */
            int sensorType = sensorItem.getSensorType();
            SensorItemAdapter.ViewHolder holder = mViewHolderMap.get(sensorType);
            if (holder != null) {
                holder.mCheckBox.setChecked(checked);
            }
        }
        mIsCollectiveOperation = false;
    }

    public void enableAllSensorTypes(boolean enabled) {
        for (int i = 0, n = mSensorItemList.size(); i < n; i++) {
            SensorItem sensorItem = mSensorItemList.get(i);

            int sensorType = sensorItem.getSensorType();
            SensorItemAdapter.ViewHolder holder = mViewHolderMap.get(sensorType);
            if (holder != null) {
                holder.mIdView.setEnabled(enabled);
                holder.mCheckBox.setEnabled(enabled);
            }
        }
    }

    public void addItem(Integer sensorType, String sensorTypeName) {
        SensorItem sensorItem = new SensorItem(sensorType, sensorTypeName);
        mSensorItemList.add(sensorItem);
        notifyItemInserted(mSensorItemList.size() - 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final CheckBox mCheckBox;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = view.findViewById(R.id.item_number);
            mCheckBox = view.findViewById(R.id.checkBox);
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + mCheckBox.getText() + "'";
        }
    }

    public interface SensorItemAdapterListener {
        void onItemCheckStateChanged(int checkedItemCount, int totalItemCount);

        void onError(String description);
    }
}
