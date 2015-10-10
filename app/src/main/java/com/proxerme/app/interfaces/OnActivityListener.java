package com.proxerme.app.interfaces;

import android.support.v4.app.Fragment;

import com.proxerme.app.activity.DashboardActivity;

/**
 * An interfaces, which is called by the {@link DashboardActivity}, if one of the in the method
 * specified event happens. All {@link Fragment}s should implement this.
 *
 * @author Ruben Gees
 */
public interface OnActivityListener {
    /**
     * A Method that Fragments should implement. It is called, when the user clicks the `back`
     * Button.
     *
     * @return true, if the call was consumed by the Fragment or false if it was not and the caller
     * should consume it.
     */
    boolean onBackPressed();

    void showErrorIfNecessary();
}
