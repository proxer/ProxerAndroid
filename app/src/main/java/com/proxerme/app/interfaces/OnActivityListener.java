package com.proxerme.app.interfaces;

import com.proxerme.app.activity.DashboardActivity;

/**
 * An interfaces, which is called by the {@link DashboardActivity}, if one of the in the interface
 * specified event happens. All Fragments should implement this.
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

    /**
     * A method which notify implementing classes that they should reshow an Error if existing.
     * This is called on an orientation change for example.
     */
    void showErrorIfNecessary();
}
