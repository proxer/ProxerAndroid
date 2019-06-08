@file:Suppress("NOTHING_TO_INLINE", "FunctionMinLength")

package me.proxer.app.util.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.children

@ColorInt
inline fun Context.resolveColor(
    @AttrRes res: Int,
    resourceTheme: Resources.Theme = theme,
    resolveRefs: Boolean = true
) = TypedValue()
    .apply {
        if (!resourceTheme.resolveAttribute(res, this, resolveRefs)) {
            error("Could not resolve $res")
        }
    }
    .let {
        when {
            it.resourceId != 0 -> ContextCompat.getColor(this, it.resourceId)
            it.data != 0 -> it.data
            else -> error("Could not resolve $res")
        }
    }

@CheckResult
inline fun <reified T : Any> Context.intentFor(vararg params: Pair<String, Any?>): Intent {
    val intent = Intent(this, T::class.java)
    params.forEach { intent.putExtras(bundleOf(it)) }
    return intent
}

inline fun <reified T : Activity> Context.startActivity(vararg params: Pair<String, Any?>) =
    startActivity(intentFor<T>(*params))

inline fun Context.toast(message: Int, duration: Int = Toast.LENGTH_LONG): Toast = Toast
    .makeText(this, message, duration)
    .apply { show() }

inline fun Context.toast(message: String, duration: Int = Toast.LENGTH_LONG): Toast = Toast
    .makeText(this, message, duration)
    .apply { show() }

val ViewGroup.recursiveChildren: Sequence<View>
    get() = children.flatMap {
        if (it is ViewGroup) {
            sequenceOf(it) + it.recursiveChildren
        } else {
            sequenceOf(it)
        }
    }

inline fun Intent.newTask(): Intent = apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
inline fun Intent.clearTop(): Intent = apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }

inline fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
inline fun Context.dip(value: Float): Int = (value * resources.displayMetrics.density).toInt()
inline fun Context.sp(value: Int): Int = (value * resources.displayMetrics.scaledDensity).toInt()
inline fun Context.sp(value: Float): Int = (value * resources.displayMetrics.scaledDensity).toInt()

inline fun View.dip(value: Int): Int = context.dip(value)
inline fun View.dip(value: Float): Int = context.dip(value)
inline fun View.sp(value: Int): Int = context.sp(value)
inline fun View.sp(value: Float): Int = context.sp(value)
