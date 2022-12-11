package com.example.samplepublisher.ui.main;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplepublisher.MainActivity;
import com.example.samplepublisher.R;
import com.example.samplepublisher.constants.BundleKeys;
import com.example.samplepublisher.models.SensorItem;
import com.example.samplepublisher.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment handling Sensors on the device.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class MainFragment extends Fragment {
    private final static String TAG = MainFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener = null;
    //private SensorViewModel mViewModel;
    private long mSentCount = 0L;
    private final DateTimeUtil mDateTimeUtil = new DateTimeUtil();
    private boolean mIsPartialUnchecked = false;
    private boolean mMonitorCellularSignalStrength = false;
    private String mLocationProvider = null;
    private boolean mIsCellularDebug = false;
    private boolean mIsLocationDebug = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public static MainFragment newInstance() {
        return new MainFragment();
    }

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     *
     * @param context The Context which implements SendFragmentListener
     */
    @Override
    public void onAttach(@NonNull Context context) {
        Log.d(TAG, "onAttach");
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context +
                    " must implement OnFragmentInteractionListener");
        }
    }

    /**
     * Called when the fragment is no longer attached to its activity.  This
     * is called after {@link #onDestroy()}.
     */
    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        mListener = null;
        super.onDetach();
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
     * {@link Lifecycle.State#CREATED} callback.
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
         * Here we assume that calling Activity will provide the
         * service name for this MQTT session.
         */
        Bundle bundle = getArguments();
        if (bundle != null) {
            mMonitorCellularSignalStrength =
                    bundle.getBoolean(BundleKeys.BUNDLE_KEY_CELLULAR,
                            false);
            mIsCellularDebug =
                    bundle.getBoolean(BundleKeys.BUNDLE_KEY_CELLULAR_DEBUG,
                            false);

            String locationProvider =
                    bundle.getString(BundleKeys.BUNDLE_KEY_LOCATION_PROVIDER);
            if (locationProvider != null) {
                mLocationProvider = locationProvider;
            }
            mIsLocationDebug =
                    bundle.getBoolean(BundleKeys.BUNDLE_KEY_LOCATION_DEBUG,
                            false);
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
        // return super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_main, container, false);
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

        /* Setup for each view component */
        SensorItemAdapter sensorItemAdapter = setupSensorItemAdapter(view);
        setupCheckBoxSelectAll(view, sensorItemAdapter);
        setupRecyclerView(view, sensorItemAdapter);
        setupCellularPanel(view);
        setupLocationPanel(view);
        setupToggleButton(view);
        setupResetButton(view);
    }

    private void setupCheckBoxSelectAll(
            @NonNull View view, @NonNull SensorItemAdapter sensorItemAdapter) {
        CheckBox checkBoxSelectAll = view.findViewById(R.id.checkBoxSelectAll);
        if (checkBoxSelectAll != null) {
            checkBoxSelectAll.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(
                                CompoundButton buttonView, boolean isChecked) {
                            Log.d(TAG, "CheckBoxSelectAll: isChecked=" + isChecked);
                            if (mIsPartialUnchecked) {
                                Log.d(TAG, "Uncheck item one by one");
                            } else {
                                sensorItemAdapter.checkAllSensorTypes(isChecked);
                                enableSensorOnOffButton(isChecked);
                            }
                        }
                    });
        } else {
            Log.e(TAG, "CheckBoxSelectAll not found?");
        }
    }

    private void setupRecyclerView(
            @NonNull View view, @NonNull SensorItemAdapter sensorItemAdapter) {
        RecyclerView recyclerView = view.findViewById(R.id.sensorItemList);
        if (recyclerView != null) {
            Context context = recyclerView.getContext();
            LinearLayoutManager llm = new LinearLayoutManager(context);
            recyclerView.setLayoutManager(llm);
            recyclerView.setAdapter(sensorItemAdapter);
        } else {
            Log.e(TAG, "RecyclerView not found?");
        }
    }

    private SensorItemAdapter setupSensorItemAdapter(@NonNull View view) {
        // Build an empty SensorItemList. It's contents will be dynamically updated.
        List<SensorItem> arrayList = new ArrayList<>();
        SensorItemAdapter sensorItemAdapter =
                new SensorItemAdapter(
                        arrayList,
                        new SensorItemAdapter.SensorItemAdapterListener() {
                            @Override
                            public void onItemCheckStateChanged(
                                    int checkedItemCount, int totalItemCount) {
                                Log.d(TAG, "onItemCheckStateChanged: " +
                                        "checked(" + checkedItemCount + ")" +
                                        "/" +
                                        "total(" + totalItemCount + ")");

                                CheckBox checkBoxSelectAll =
                                        view.findViewById(R.id.checkBoxSelectAll);
                                if (checkBoxSelectAll != null) {
                                    if (checkedItemCount < 0
                                            || (checkedItemCount > totalItemCount)) {
                                        String message = "Invalid CheckBox state?\n" +
                                                "checked(" + checkedItemCount + ")" +
                                                "/" +
                                                "total(" + totalItemCount + ")";
                                        mListener.onError(TAG + ": " + message);
                                        return;
                                    }

                                    if (checkedItemCount == 0) {
                                        enableSensorOnOffButton(false);
                                    } else if (checkedItemCount < totalItemCount) {
                                        if (checkBoxSelectAll.isChecked()) {
                                            mIsPartialUnchecked = true;
                                            checkBoxSelectAll.setChecked(false);
                                            mIsPartialUnchecked = false;
                                        }
                                        enableSensorOnOffButton(true);
                                    } else {
                                        checkBoxSelectAll.setChecked(true);
                                        enableSensorOnOffButton(true);
                                    }
                                }
                            }

                            @Override
                            public void onError(String description) {
                                mListener.onError(description);
                            }
                        }
                );

        return sensorItemAdapter;
    }

    private void setupCellularPanel(@NonNull View view) {
        ConstraintLayout cellularPanel = view.findViewById(R.id.cellularPanel);
        if (cellularPanel != null) {
            if (mMonitorCellularSignalStrength) {
                cellularPanel.setVisibility(View.VISIBLE);
            } else {
                cellularPanel.setVisibility(View.GONE);
            }
        } else {
            Log.e(TAG, "CellularPanel not found?");
        }
    }

    private void setupLocationPanel(@NonNull View view) {
        ConstraintLayout locationPanel = view.findViewById(R.id.locationPanel);
        if (mLocationProvider != null) {
            /* Show location panel */
            if (locationPanel != null) {
                locationPanel.setVisibility(View.VISIBLE);
            }

            TextView tv;
            tv= view.findViewById(R.id.location_provider);
            if (tv != null) {
                tv.setText(mLocationProvider);
            }

            tv = view.findViewById(R.id.location_value);
            if (tv != null) {
                tv.setText("N/A");
            }
        } else {
            /* Hide location panel */
            if (locationPanel != null) {
                locationPanel.setVisibility(View.GONE);
            }
        }
    }

    private void setupToggleButton(@NonNull View view) {
        ToggleButton toggleButton = view.findViewById(R.id.toggleButton);
        if (toggleButton != null) {
            toggleButton.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(
                                CompoundButton buttonView, boolean isChecked) {
                            Log.d(TAG, "onCheckedChanged: " + isChecked);

                            CheckBox checkBoxSelectAll =
                                    view.findViewById(R.id.checkBoxSelectAll);
                            if (checkBoxSelectAll != null) {
                                /* Prevent touching sensor list while running */
                                checkBoxSelectAll.setEnabled(!isChecked);
                            }

                            /* If isChecked is true, call SensorManager for selected types */
                            if (isChecked) {
                                mListener.onEnableSensors(); /* RUN */
                            } else {
                                mListener.onDisableSensors(); /* STOP */
                            }
                            enableResetButton(!isChecked);
                        }
                    });
        } else {
            Log.e(TAG, "ToggleButton not found?");
        }
    }

    private void setupResetButton(@NonNull View view) {
        ImageButton resetButton = view.findViewById(R.id.resetButton);
        if (resetButton != null) {
            resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetStatistics();
                }
            });
        } else {
            Log.e(TAG, "ResetButton not found?");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //mViewModel = new ViewModelProvider(this).get(SensorViewModel.class);
        resetStatistics();
    }

    public void onSensorDataReceived(String data) {
        //mViewModel.setSensorData(data);

        /* Maybe we can monitor the sending JSON data on an UI component. */
        mSentCount++;
        updateStatistics();
    }

    public void enableSensorOnOffButton(boolean enabled) {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ToggleButton toggleButton = activity.findViewById(R.id.toggleButton);
            if (toggleButton != null) {
                /*
                 * Control SensorDevice ON/OFF.
                 * While it's ON, follow ToggleButton.setOnCheckedChangeListener.
                 */
                toggleButton.setEnabled(enabled);
            }
        }
    }

    public void showProgressBar(boolean enabled) {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            LinearLayout progressBar = activity.findViewById(R.id.progressBar);
            if (progressBar != null) {
                progressBar.setVisibility(enabled ? View.VISIBLE : View.GONE);
            }
        }
    }

    public void showEmptyMessage(boolean isEmpty) {
        Activity activity = getActivity();
        if (activity != null) {
            if (isEmpty) {
                TextView emptyView =
                        activity.findViewById(R.id.emptyView);
                if (emptyView != null) {
                    emptyView.setVisibility(View.VISIBLE);
                }
            } else {
                View sensorPanel =
                        activity.findViewById(R.id.sensorPanel);
                if (sensorPanel != null) {
                    sensorPanel.setVisibility(View.VISIBLE);
                }
            }
        } else {
            String message = TAG + ": showEmptyMessage: Activity not found?";
            mListener.onError(message);
        }
    }

    public void updateCellularInfo(@NonNull String networkType, @NonNull String data) {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            TextView tvNetworkType = activity.findViewById(R.id.network_type);
            if (tvNetworkType != null) {
                tvNetworkType.setText(networkType);
            }

            TextView tvCellularValue = activity.findViewById(R.id.cellular_value);
            if (tvCellularValue != null) {
                tvCellularValue.setText(data);
            }
        }
    }

    public void updateLocationProviders(@NonNull String providers) {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            TextView tv = activity.findViewById(R.id.location_provider);
            if (tv != null) {
                tv.setText(providers);
            }
        }
    }

    public void updateLocationValue(@NonNull String locationValue) {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            TextView tvLocation = activity.findViewById(R.id.location_value);
            if (tvLocation != null) {
                tvLocation.setText(locationValue);
            }
        }
    }

    private void updateStatistics() {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            TextView tvTimeStamp = activity.findViewById(R.id.stats_timestamp_value);
            if (tvTimeStamp != null) {
                if (mSentCount > 0L) {
                    long unixTime = mDateTimeUtil.getUnixTime();
                    String timestamp = mDateTimeUtil.toIso8601String(unixTime);
                    tvTimeStamp.setText(timestamp);
                } else {
                    tvTimeStamp.setText("N/A");
                }
            }
            TextView tvCounter = activity.findViewById(R.id.stats_counter_value);
            if (tvCounter != null) {
                String strval = "" + mSentCount;
                tvCounter.setText(strval);
            }
        }
    }

    private void resetStatistics() {
        mSentCount = 0L;
        updateStatistics();
    }

    private void enableResetButton(boolean enabled) {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ImageButton resetButton = activity.findViewById(R.id.resetButton);
            if (resetButton != null) {
                resetButton.setEnabled(enabled);
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onEnableSensors();

        void onDisableSensors();

        void onError(@NonNull String description);
    }
}
