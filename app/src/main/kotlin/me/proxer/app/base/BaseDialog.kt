package me.proxer.app.base

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.support.v4.app.DialogFragment
import me.proxer.app.MainApplication.Companion.refWatcher

/**
 * @author Ruben Gees
 */
abstract class BaseDialog : DialogFragment(), LifecycleRegistryOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }

    override fun onDestroy() {
        super.onDestroy()

        refWatcher.watch(this)
    }

    override fun getLifecycle() = lifecycleRegistry
}
