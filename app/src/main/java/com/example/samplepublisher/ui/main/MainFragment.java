package com.example.samplepublisher.ui.main;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private String mLocationProvider = null;

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
            throw new RuntimeException(context.toString() +
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
            String locationProvider =
                    bundle.getString(BundleKeys.BUNDLE_KEY_LOCATION_PROVIDER);
            if (locationProvider != null) {
                mLocationProvider = locationProvider;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        RecyclerView recyclerView = rootView.findViewById(R.id.sensorItemList);
        if (recyclerView != null) {
            // Set the adapter
            Context context = recyclerView.getContext();
            LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(context);
            recyclerView.setLayoutManager(mLinearLayoutManager);

            // Build an empty SensorItemList. It's contents will be dynamically updated.
            List<SensorItem> arrayList = new ArrayList<>();
            recyclerView.setAdapter(new SensorItemAdapter(arrayList, mListener));
        } else {
            Log.e(TAG, "RecyclerView not found?");
        }

        ConstraintLayout locationPanel = rootView.findViewById(R.id.locationPanel);
        if (mLocationProvider != null) {
            /* Show location panel */
            if (locationPanel != null) {
                locationPanel.setVisibility(View.VISIBLE);
            }

            TextView tv;
            tv= rootView.findViewById(R.id.location_provider);
            if (tv != null) {
                tv.setText(mLocationProvider);
            }

            tv = rootView.findViewById(R.id.location_value);
            if (tv != null) {
                tv.setText("N/A");
            }
        } else {
            /* Hide location panel */
            if (locationPanel != null) {
                locationPanel.setVisibility(View.GONE);
            }
        }

        ToggleButton toggleButton = rootView.findViewById(R.id.toggleButton);
        if (toggleButton != null) {
            toggleButton.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                            Log.d(TAG, "onCheckedChanged: " + isChecked);

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

        ImageButton resetButton = rootView.findViewById(R.id.resetButton);
        if (resetButton != null) {
            resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetStatistics();
                }
            });
        }
        return rootView;
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
            RecyclerView recyclerView =
                    activity.findViewById(R.id.sensorItemList);
            TextView emptyView =
                    activity.findViewById(R.id.emptyView);

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

            ConstraintLayout controlBar = activity.findViewById(R.id.controlBar);
            if (controlBar != null) {
                controlBar.setVisibility(View.VISIBLE);
            }
        } else {
            String message = TAG + ": showEmptyMessage: Activity not found?";
            mListener.onError(message);
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
        void onSensorTypesChecked(boolean checked);

        void onEnableSensors();

        void onDisableSensors();

        void onError(@NonNull String message);
    }
}
