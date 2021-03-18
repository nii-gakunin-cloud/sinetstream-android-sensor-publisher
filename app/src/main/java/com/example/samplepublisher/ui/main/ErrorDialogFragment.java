package com.example.samplepublisher.ui.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.samplepublisher.R;
import com.example.samplepublisher.constants.BundleKeys;

public class ErrorDialogFragment extends DialogFragment {
    private String mErrorMessage;

    private final Activity mActivity;
    private ErrorDialogListener mListener = null;
    private Parcelable mCallbackParcelable = null;
    private boolean mIsFatal = false;

    public ErrorDialogFragment(Activity activity) {
        // Required empty public constructor
        this.mActivity = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = mActivity;
        if (context instanceof ErrorDialogListener) {
            mListener = (ErrorDialogListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ErrorDialogListener");
        }

        Bundle bundle = getArguments();
        if (bundle != null) {
            mErrorMessage = bundle.getString(BundleKeys.BUNDLE_KEY_ERROR_MESSAGE);
            mCallbackParcelable = bundle.getParcelable(BundleKeys.BUNDLE_KEY_PARCELABLE);
            mIsFatal = bundle.getBoolean(BundleKeys.BUNDLE_KEY_ERROR_FATAL);
        } else {
            mErrorMessage = "N/A";
        }
    }

    /*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView textView = new TextView(getActivity());
        textView.setText(R.string.hello_blank_fragment);
        return textView;
    }
    */

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // return super.onCreateDialog(savedInstanceState);

        // Use the Builder class for convenient dialog construction
        // Use the default AppTheme for the AlertDialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        builder.setTitle(getString(R.string.dialog_title_error));
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setMessage(mErrorMessage)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /*
                         * Let calling Activity do the rest of tasks.
                         */
                        if (mListener != null) {
                            mListener.onErrorDialogDismissed(mCallbackParcelable, mIsFatal);
                        }
                    }
                });

        // Create an AlertDialog object and return it
        Dialog alertDialog = builder.create();

        // Don't allow user to cancel by touching outside of the dialog.
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        return alertDialog;
    }

    public interface ErrorDialogListener {
        void onErrorDialogDismissed(@Nullable Parcelable parcelable, boolean isFatal);

        //void onNegativeButtonPressed();
    }
}
