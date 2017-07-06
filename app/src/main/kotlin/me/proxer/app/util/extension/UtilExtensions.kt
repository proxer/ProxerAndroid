@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import me.proxer.app.R
import me.proxer.app.activity.WebViewActivity
import me.proxer.app.entity.manga.LocalMangaChapter
import me.proxer.app.util.Utils
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.entitiy.manga.Page
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import java.net.URLDecoder
import java.util.concurrent.Semaphore

inline val Page.decodedName: String
    get() = try {
        URLDecoder.decode(name, "UTF-8")
    } catch (error: Throwable) {
        ""
    }

fun CustomTabsHelperFragment.openHttpPage(activity: Activity, url: HttpUrl) {
    when (Utils.getNativeAppPackage(activity, url).isEmpty()) {
        true -> {
            CustomTabsIntent.Builder(session)
                    .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
                    .addDefaultShareMenuItem()
                    .enableUrlBarHiding()
                    .setShowTitle(true)
                    .build()
                    .let {
                        CustomTabsHelperFragment.open(activity, it, url.androidUri(), { context, uri ->
                            WebViewActivity.navigateTo(context, uri.toString())
                        })
                    }
        }
        false -> activity.startActivity(Intent(Intent.ACTION_VIEW).setData(url.androidUri()))
    }
}

inline fun Context.getDrawableFromAttrs(resource: Int): Drawable {
    val styledAttributes = obtainStyledAttributes(intArrayOf(resource))
    val result = styledAttributes.getDrawable(0)

    styledAttributes.recycle()

    return result
}

inline fun Context.getQuantityString(id: Int, quantity: Int): String {
    return this.resources.getQuantityString(id, quantity, quantity)
}

inline fun HttpUrl.androidUri(): Uri {
    return Uri.parse(toString())
}

inline fun <T> Semaphore.lock(action: () -> T): T {
    acquire()

    return try {
        action()
    } finally {
        release()
    }
}

typealias CompleteLocalMangaEntry = Pair<EntryCore, List<LocalMangaChapter>>