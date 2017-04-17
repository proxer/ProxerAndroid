package me.proxer.app.util.extension

import android.app.Activity
import android.app.Dialog
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.View
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import me.proxer.app.adapter.base.PagingAdapter
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object KotterKnife {
    fun reset(target: Any) {
        LazyRegistry.reset(target)
    }
}

fun <V : View> View.bindView(id: Int): ReadOnlyProperty<View, V> = required(id, viewFinder)
fun <V : View> Activity.bindView(id: Int): ReadOnlyProperty<Activity, V> = required(id, viewFinder)
fun <V : View> Dialog.bindView(id: Int): ReadOnlyProperty<Dialog, V> = required(id, viewFinder)
fun <V : View> Fragment.bindView(id: Int): ReadOnlyProperty<Fragment, V> = required(id, viewFinder)
fun <V : View> ViewHolder.bindView(id: Int): ReadOnlyProperty<ViewHolder, V> = required(id, viewFinder)
fun <V : View> DialogFragment.bindView(id: Int): ReadOnlyProperty<DialogFragment, V> = required(id, viewFinder)

@Suppress("unused")
private val View.viewFinder: View.(Int) -> View?
    get() = { findViewById(it) }

@Suppress("unused")
private val Activity.viewFinder: Activity.(Int) -> View?
    get() = { findViewById(it) }

@Suppress("unused")
private val Dialog.viewFinder: Dialog.(Int) -> View?
    get() = { findViewById(it) }

@Suppress("unused")
private val Fragment.viewFinder: Fragment.(Int) -> View?
    get() = { view!!.findViewById(it) }

@Suppress("unused")
private val ViewHolder.viewFinder: ViewHolder.(Int) -> View?
    get() = { itemView.findViewById(it) }

@Suppress("unused")
private val DialogFragment.viewFinder: DialogFragment.(Int) -> View?
    get() = { dialog.findViewById(it) }

private fun viewNotFound(id: Int, desc: KProperty<*>): Nothing =
        throw IllegalStateException("View ID $id for '${desc.name}' not found.")

@Suppress("UNCHECKED_CAST")
private fun <T, V : View> required(id: Int, finder: T.(Int) -> View?)
        = Lazy { t: T, desc -> t.finder(id) as V? ?: viewNotFound(id, desc) }

// Like Kotlin's lazy delegate but the initializer gets the target and metadata passed to it.
// Also automatically destroys (removes leaking instances) the view on a call to [reset].
private class Lazy<in T, out V>(private val initializer: (T, KProperty<*>) -> V) : ReadOnlyProperty<T, V> {
    private object EMPTY

    private var value: Any? = EMPTY

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        LazyRegistry.register(thisRef!!, this)

        if (value == EMPTY) {
            value = initializer(thisRef, property)
        }

        @Suppress("UNCHECKED_CAST")
        return value as V
    }

    fun reset() {
        destroy()

        value = EMPTY
    }

    private fun destroy() {
        val safeValue = value

        when (safeValue) {
            is RecyclerView -> {
                var currentAdapter = safeValue.adapter

                if (currentAdapter is EasyHeaderFooterAdapter) {
                    currentAdapter.removeHeader()
                    currentAdapter.removeFooter()

                    currentAdapter = currentAdapter.innerAdapter
                }

                if (currentAdapter is PagingAdapter<*>) {
                    currentAdapter.destroy()
                }

                safeValue.clearOnScrollListeners()
                safeValue.layoutManager = null
                safeValue.adapter = null
            }
            is SwipeRefreshLayout -> {
                safeValue.setOnRefreshListener(null)
            }
        }
    }
}

private object LazyRegistry {
    private val lazyMap = WeakHashMap<Any, MutableCollection<Lazy<*, *>>>()

    fun register(target: Any, lazy: Lazy<*, *>) {
        lazyMap.getOrPut(target, { Collections.newSetFromMap(WeakHashMap()) }).add(lazy)
    }

    fun reset(target: Any) {
        lazyMap[target]?.forEach { it.reset() }
    }
}
