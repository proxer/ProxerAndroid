package com.proxerme.app.dialog;

import android.support.v4.app.DialogFragment;

/**
 * Todo: Describe Class
 *
 * @author Ruben Gees
 */
public class MainDialog<C> extends DialogFragment {

    private C callback;

    protected C getCallback() {
        return callback;
    }

    public void setCallback(C callback) {
        this.callback = callback;
    }
}
