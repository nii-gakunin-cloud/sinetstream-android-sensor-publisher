package com.example.samplepublisher.ui.main;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

public class SensorViewModel extends ViewModel {
    private boolean mIsSensorRunning = false;
    private String mPrivateKeyAlias = null;

    public boolean isSensorRunning() {
        return mIsSensorRunning;
    }

    public void setSensorRunning(boolean isRunning) {
        this.mIsSensorRunning = isRunning;
    }

    @Nullable
    public String getPrivateKeyAlias() {
        return mPrivateKeyAlias;
    }

    public void setPrivateKeyAlias(@Nullable String alias) {
        this.mPrivateKeyAlias = alias;
    }
}
