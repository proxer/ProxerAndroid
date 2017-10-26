package me.proxer.app.base

import android.support.v4.app.DialogFragment
import kotterknife.KotterKnife
import me.proxer.app.MainApplication.Companion.refWatcher

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseDialog : DialogFragment() {

    val safeContext get() = context ?: throw IllegalStateException("context is null")
    val safeActivity get() = activity ?: throw IllegalStateException("activity is null")
    val safeArguments get() = arguments ?: throw IllegalStateException("arguments are null")
    val safeTargetFragment get() = targetFragment ?: throw IllegalStateException("targetFragment is null")

    override fun onDestroyView() {
        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

        refWatcher.watch(this)
    }
}
