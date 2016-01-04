package com.proxerme.app.dialog;

import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

/**
 * An abstract dialog, which all dialogs with a Callback should inherit from.
 *
 * @author Ruben Gees
 */
public class MainDialog<C> extends DialogFragment {

    private C callback;

    @Nullable
    protected C getCallback() {
        return callback;
    }

    public void setCallback(@Nullable C callback) {
        this.callback = callback;
    }
}
