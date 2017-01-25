package com.proxerme.app.util

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.proxerme.app.R
import com.proxerme.app.activity.WebViewActivity
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.library.connection.manga.entity.Page
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import java.net.URLDecoder

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */

val Context.inputMethodManager: InputMethodManager
    get() = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

val Context.alarmManager: AlarmManager
    get() = getSystemService(Context.ALARM_SERVICE) as AlarmManager

val Context.windowManager: WindowManager
    get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

fun CustomTabsHelperFragment.openHttpPage(activity: Activity, url: HttpUrl) {
    if (Utils.getNativeAppPackage(activity, url).isEmpty()) {
        val customTabsIntent = CustomTabsIntent.Builder(session)
                .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setSecondaryToolbarColor(ContextCompat.getColor(context,
                        R.color.colorPrimaryDark))
                .addDefaultShareMenuItem()
                .enableUrlBarHiding()
                .setShowTitle(true)
                .build()

        CustomTabsHelperFragment.open(activity, customTabsIntent, url.androidUri(),
                { activity, uri ->
                    WebViewActivity.navigateTo(activity, uri.toString())
                })
    } else {
        activity.startActivity(Intent(Intent.ACTION_VIEW).setData(url.androidUri()))
    }
}

fun <T> PagingAdapter<T>.updateAndScrollUpIfNecessary(layoutManager: RecyclerView.LayoutManager,
                                                      recyclerView: RecyclerView,
                                                      action: (it: PagingAdapter<T>) -> Unit) {
    val previousFirstItem = this.items.firstOrNull()
    val wasAtTop = when (layoutManager) {
        is LinearLayoutManager -> layoutManager.findFirstVisibleItemPosition() == 0
        is StaggeredGridLayoutManager -> {
            layoutManager.findFirstVisibleItemPositions(null).contains(0)
        }
        else -> throw IllegalArgumentException("Unknown LayoutManager: $layoutManager")
    }

    action.invoke(this)

    if (wasAtTop && previousFirstItem != this.items.firstOrNull()) {
        recyclerView.post {
            recyclerView.smoothScrollToPosition(0)
        }
    }
}

fun HttpUrl.androidUri(): Uri {
    return Uri.parse(toString())
}

fun <T : View> View.findChild(predicate: (View) -> Boolean): T? {
    if (this !is ViewGroup) return null

    for (i in 0 until childCount) {
        if (predicate.invoke(getChildAt(i))) {
            @Suppress("UNCHECKED_CAST")
            return getChildAt(i) as T
        }

        getChildAt(i).findChild<T>(predicate)?.let {
            return it
        }
    }

    return null
}

val Page.decodedName: String
    get() = try {
        URLDecoder.decode(name, "UTF-8")
    } catch (exception: Exception) {
        ""
    }