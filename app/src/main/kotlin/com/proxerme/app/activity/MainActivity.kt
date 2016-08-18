package com.proxerme.app.activity

import android.support.v7.app.AppCompatActivity
import com.proxerme.app.helper.PreferenceHelper

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class MainActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()

        setNightMode()
    }

    fun setNightMode() {
        delegate.setLocalNightMode(PreferenceHelper.getNightMode(this))
    }
}