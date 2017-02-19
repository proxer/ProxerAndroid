package com.proxerme.app.helper

import android.content.Context
import com.proxerme.app.R

object ScreenHelper {

    fun isTablet(context: Context): Boolean {
        return context.resources.getString(R.string.screen_type) == "tablet"
    }
}