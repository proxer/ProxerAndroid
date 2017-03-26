@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import me.proxer.app.R
import me.proxer.app.activity.WebViewActivity
import me.proxer.app.util.Utils
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl

//inline val Page.decodedName: String
//    get() = try {
//        URLDecoder.decode(name, "UTF-8")
//    } catch (exception: Exception) {
//        ""
//    }

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

inline fun HttpUrl.androidUri(): Uri {
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
