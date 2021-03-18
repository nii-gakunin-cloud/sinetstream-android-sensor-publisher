package com.example.samplepublisher.ui.main;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.samplepublisher.R;

public class InProgressDialogFragment extends DialogFragment {
    private ProgressDialogListener mListener;
    private final String mTitle;

    public InProgressDialogFragment(@Nullable String title) {
        this.mTitle = title;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        if (context instanceof ProgressDialogListener) {
            mListener = (ProgressDialogListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ProgressDialogListener");
        }
    }

    /**
     * Override to build your own custom Dialog container.  This is typically
     * used to show an AlertDialog instead of a generic Dialog; when doing so,
     * {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)} does not need
     * to be implemented since the AlertDialog takes care of its own content.
     *
     * <p>This method will be called after {@link #onCreate(Bundle)} and
     * before {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.  The
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_inprogress, null));

        /*
         * For now, we cannot handle canceling of ongoing connect request
         * in clean way. XXX
         *
        // Add action buttons
        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                InProgressDialogFragment.this.getDialog().cancel();
                mListener.onCanceled();
            }
        });
        */

        if (mTitle != null) {
            builder.setTitle(mTitle);
        }

        // Create an AlertDialog object and return it
        Dialog alertDialog = builder.create();

        // Don't allow user to cancel by touching outside of the dialog.
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        return alertDialog;
    }

    public interface ProgressDialogListener {
        void onCanceled();
    }
}
