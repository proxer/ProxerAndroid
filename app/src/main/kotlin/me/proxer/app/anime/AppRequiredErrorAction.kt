package me.proxer.app.anime

import android.support.v7.app.AppCompatActivity
import me.proxer.app.util.ErrorUtils

/**
 * @author Ruben Gees
 */
class AppRequiredErrorAction(private val name: String, private val appPackage: String) : ErrorUtils.ErrorAction(0) {
    fun showDialog(activity: AppCompatActivity) = AppRequiredDialog.show(activity, name, appPackage)
}
