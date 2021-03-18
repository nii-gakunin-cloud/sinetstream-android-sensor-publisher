package com.example.samplepublisher.net;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import jp.ad.sinet.stream.android.api.SinetStreamWriter;
import jp.ad.sinet.stream.android.api.ValueType;

/**
 * Provides a set of API functions to be a Writer (= publisher)
 * in the SINETStream system.
 * <p>
 *     This class extends the generic {@link SinetStreamWriter}
 *     to handle String type user data.
 * </p>
 */
public class SinetStreamWriterString extends SinetStreamWriter<String> {
    private final String TAG = SinetStreamWriterString.class.getSimpleName();

    /**
     * Constructs a SinetStreamWriterString instance.
     *
     * @param context the Application context which implements
     *                {@link SinetStreamWriterListener},
     *                usually it is the calling {@link Activity} itself.
     * @throws RuntimeException if given context does not implement
     *                          the required listener.
     */
    public SinetStreamWriterString(@NonNull Context context) {
        super(context);
    }

    /**
     * Sets up oneself as a Writer which handles String type.
     *
     * @param serviceName the service name to match configuration parameters.
     * @see SinetStreamWriter
     */
    @Override
    public void initialize(@NonNull String serviceName) {
        super.initialize(serviceName);

        if (super.isInitializationSuccess()) {
            ValueType valueType = getValueType();
            if (valueType != null && valueType.equals(ValueType.TEXT)) {
                super.setup();
            } else {
                super.abort(TAG + ": ValueType mismatch");
            }
        }
    }

    public interface SinetStreamWriterStringListener
            extends SinetStreamWriterListener<String> {

    }
}
