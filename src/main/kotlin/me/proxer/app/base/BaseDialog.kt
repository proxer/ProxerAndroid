package me.proxer.app.base

import android.support.v4.app.DialogFragment
import kotterknife.KotterKnife
import me.proxer.app.MainApplication.Companion.refWatcher

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseDialog : DialogFragment() {

    override fun onDestroyView() {
        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

        refWatcher.watch(this)
    }
}
