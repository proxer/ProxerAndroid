package me.proxer.app.anime

import android.support.v7.app.AppCompatActivity
import me.proxer.app.util.ErrorUtils

/**
 * @author Ruben Gees
 */
class AppRequiredErrorAction(val name: String, val appPackage: String) : ErrorUtils.ErrorAction(0) {

    fun showDialog(activity: AppCompatActivity) = AppRequiredDialog.show(activity, name, appPackage)
}