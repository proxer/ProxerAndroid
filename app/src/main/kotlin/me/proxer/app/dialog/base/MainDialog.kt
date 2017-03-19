package me.proxer.app.dialog.base

import android.support.v4.app.DialogFragment
import me.proxer.app.application.MainApplication

/**
 * @author Ruben Gees
 */
abstract class MainDialog : DialogFragment() {

    override fun onDestroy() {
        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }
}
