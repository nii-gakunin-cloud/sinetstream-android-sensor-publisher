package com.example.samplepublisher.util;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.samplepublisher.R;
import com.example.samplepublisher.constants.BundleKeys;
import com.example.samplepublisher.ui.main.ErrorDialogFragment;

public class DialogUtil {
    /**
     * Helper function to show an ErrorDialog.
     * Don't forget to implement the interface ErrorDialogFragment#ErrorDialogListener.
     *
     * @param activity    the calling Activity
     * @param errorMessage    an error message to be shown
     * @param callbackParcelable    optional opaque data
     * @param isFatal    shall the calling Activity terminate after the message?
     */
    public static void showErrorDialog(
            AppCompatActivity activity,
            @Nullable String errorMessage,
            @Nullable Parcelable callbackParcelable,
            boolean isFatal) {
        ErrorDialogFragment edf = new ErrorDialogFragment(activity);
        Bundle bundle = new Bundle();
        if (errorMessage != null) {
            bundle.putString(BundleKeys.BUNDLE_KEY_ERROR_MESSAGE, errorMessage);
        }
        if (callbackParcelable != null) {
            bundle.putParcelable(BundleKeys.BUNDLE_KEY_PARCELABLE, callbackParcelable);
        }
        bundle.putBoolean(BundleKeys.BUNDLE_KEY_ERROR_FATAL, isFatal);
        edf.setArguments(bundle);
        edf.show(activity.getSupportFragmentManager(), "ERROR");
    }

    public static void showSimpleDialog(
            AppCompatActivity activity, @NonNull String descriptions, boolean isHtml) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.app_name);
        builder.setIcon(R.mipmap.ic_launcher);
        if (isHtml) {
            builder.setMessage(Html.fromHtml(descriptions, Html.FROM_HTML_MODE_COMPACT));
        } else {
            builder.setMessage(descriptions);
        }
        builder.setPositiveButton(
                android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        // Create an AlertDialog object and return it
        Dialog alertDialog = builder.create();

        // Don't allow user to cancel by touching outside of the dialog.
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        if (isHtml) {
            ((TextView) alertDialog.findViewById(android.R.id.message)).
                    setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
}
