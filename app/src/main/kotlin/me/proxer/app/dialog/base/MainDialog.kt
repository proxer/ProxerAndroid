package me.proxer.app.dialog.base

import android.support.v4.app.DialogFragment
import me.proxer.app.application.MainApplication
import me.proxer.app.util.extension.KotterKnife

/**
 * @author Ruben Gees
 */
abstract class MainDialog : DialogFragment() {

    override fun onDestroyView() {
        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }
}
