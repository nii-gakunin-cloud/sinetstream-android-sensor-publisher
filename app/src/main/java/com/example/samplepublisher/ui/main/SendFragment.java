package com.example.samplepublisher.ui.main;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.samplepublisher.R;
import com.example.samplepublisher.constants.BundleKeys;
import com.example.samplepublisher.net.SinetStreamWriterString;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SendFragment} factory method to
 * create an instance of this fragment.
 */
public class SendFragment extends Fragment {
    private final static String TAG = SendFragment.class.getSimpleName();

    private String mServiceName = "";
    private SinetStreamWriterString mSinetStreamWriter = null;
    private SendFragmentListener mListener;

    public SendFragment() {
        // Required empty public constructor
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

        if (context instanceof SendFragmentListener) {
            mListener = (SendFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() +
                    " must implement SendFragmentListener");
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
     * created, see {@link #onActivityCreated(Bundle)}.
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
            String serviceName = bundle.getString(BundleKeys.BUNDLE_KEY_SERVICE_NAME);
            if (serviceName != null) {
                mServiceName = serviceName;
            }
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null. This will be called between
     * {@link #onCreate(Bundle)} and {@link #onActivityCreated(Bundle)}.
     * <p>A default View can be returned by calling {@link #Fragment} in your
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: savedInstanceState=" + savedInstanceState);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sender, container, false);
    }

    /**
     * Called when the fragment's activity has been created and this
     * fragment's view hierarchy instantiated.  It can be used to do final
     * initialization once these pieces are in place, such as retrieving
     * views or restoring state.  It is also useful for fragments that use
     * {@link #setRetainInstance(boolean)} to retain their instance,
     * as this callback tells the fragment when it is fully associated with
     * the new activity instance.  This is called after {@link #onCreateView}
     * and before {@link #onViewStateRestored(Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated: savedInstanceState=" + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        // mViewModel = ViewModelProvider.of(this).get(MainViewModel.class);

        Activity activity = getActivity();
        if (activity != null) {
            mSinetStreamWriter = new SinetStreamWriterString(activity);
        } else {
            // This case must not happen.
            mListener.onError(TAG + ": Cannot get Activity?");
        }
    }

    /**
     * Called when the Fragment is visible to the user.  This is generally
     * tied to Activity.onStart of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onStart() {
        Log.d(TAG,"onStart");
        super.onStart();

        /*
         * Make following part as a separate method startWriter().
         *
        if (mSinetStreamWriter != null) {
            mSinetStreamWriter.initialize(mServiceName);
        }
         */
    }

    /**
     * Called when the Fragment is no longer started.  This is generally
     * tied to Activity.onStop of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onStop() {
        Log.d(TAG, "onStop");

        /*
         * Make following part as a separate method stopWriter().
         *
        if (mSinetStreamWriter != null) {
            mSinetStreamWriter.terminate();
        }
         */
        super.onStop();
    }

    public void startWriter() {
        if (mSinetStreamWriter != null) {
            mSinetStreamWriter.initialize(mServiceName);
        }
    }

    public void stopWriter() {
        if (mSinetStreamWriter != null) {
            mSinetStreamWriter.terminate();
        }
    }

    public void sendMessage(@NonNull String message) {
        Log.d(TAG, "msg=" + message);

        if (mSinetStreamWriter != null) {
            mSinetStreamWriter.publish(message, null);
        }
    }

    public void onPublished(@NonNull String message, @Nullable Object userData) {
        Log.d(TAG, "onPublished: " + "message=" + message);
    }

    /**
     * Communicate with other fragments
     * http://developer.android.com/training/basics/fragments/communicating.html
     */
    public interface SendFragmentListener {
        void onError(@NonNull String errorMessage);
    }
}
