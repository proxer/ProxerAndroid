package me.proxer.app.base

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.os.Bundle
import android.support.v4.app.DialogFragment
import kotterknife.KotterKnife
import me.proxer.app.MainApplication.Companion.refWatcher
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseDialog : DialogFragment(), LifecycleRegistryOwner {

    private var lifecycleRegistry by Delegates.notNull<LifecycleRegistry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleRegistry = LifecycleRegistry(this)
    }

    override fun onDestroyView() {
        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

        refWatcher.watch(this)
    }

    override fun getLifecycle() = lifecycleRegistry
}
