package com.rubengees.proxerme.interfaces;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public interface OnBackPressedListener {
    /**
     * A listener that Fragments should implement. It is called, when the user clicks the `back`
     * Button.
     *
     * @return true, if the call was consumed by the Fragment or false if it was nat and the caller
     * should consume it.
     */
    boolean onBackPressed();
}
