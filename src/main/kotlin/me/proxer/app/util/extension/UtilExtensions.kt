@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.util.Linkify
import android.view.View
import android.widget.ImageView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.text.util.LinkifyCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import me.proxer.app.BuildConfig.APPLICATION_ID
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.ui.WebViewActivity
import me.proxer.app.util.Utils
import me.proxer.library.util.ProxerUrls
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import org.jetbrains.anko.dip
import java.util.EnumSet

val MENTIONS_REGEX = Regex("(@[^ \n]+)").toPattern()

inline fun <reified T : Enum<T>> enumSetOf(collection: Collection<T>): EnumSet<T> = when (collection.isEmpty()) {
    true -> EnumSet.noneOf(T::class.java)
    false -> EnumSet.copyOf(collection)
}

inline fun <T> unsafeLazy(noinline initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

inline fun Context.getQuantityString(id: Int, quantity: Int): String = resources
    .getQuantityString(id, quantity, quantity)

inline fun Fragment.dip(value: Int) = context?.dip(value)
    ?: throw IllegalStateException("context is null")

inline fun CharSequence.linkify(web: Boolean = true, mentions: Boolean = true, vararg custom: Regex): Spannable {
    val spannable = this as? Spannable ?: SpannableString(this)

    if (web) LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS)
    if (mentions) LinkifyCompat.addLinks(spannable, MENTIONS_REGEX, "")

    custom.forEach {
        LinkifyCompat.addLinks(spannable, it.toPattern(), "")
    }

    return spannable
}

inline fun GlideRequests.defaultLoad(view: ImageView, url: HttpUrl): Target<Drawable> = load(url.toString())
    .transition(DrawableTransitionOptions.withCrossFade())
    .into(view)

inline fun HttpUrl.androidUri(): Uri = Uri.parse(toString())

inline fun <T : View> T.postDelayedSafely(crossinline callback: (T) -> Unit, delayMillis: Long) {
    postDelayed({ callback(this) }, delayMillis)
}

inline fun <T : Enum<T>> Bundle.putEnumSet(key: String, set: EnumSet<T>) {
    putIntArray(key, set.map { it.ordinal }.toIntArray())
}

inline fun <reified T : Enum<T>> Bundle.getEnumSet(key: String, klass: Class<T>): EnumSet<T> {
    val values = getIntArray(key)?.map { klass.enumConstants[it] }

    return when {
        values?.isEmpty() != false -> EnumSet.noneOf(T::class.java)
        else -> EnumSet.copyOf(values)
    }
}

inline fun Intent.addReferer(context: Context): Intent {
    val referrerExtraName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        Intent.EXTRA_REFERRER
    } else {
        "android.intent.extra.REFERRER"
    }

    putExtra(referrerExtraName, Uri.parse("android-app://" + context.packageName))

    return this
}

fun CustomTabsHelperFragment.openHttpPage(activity: Activity, url: HttpUrl, forceBrowser: Boolean = false) {
    if (forceBrowser) {
        doOpenHttpPage(activity, url)
    } else {
        val nativePackages = Utils.getNativeAppPackage(activity, url)

        when (nativePackages.isEmpty()) {
            true -> doOpenHttpPage(activity, url)
            false -> {
                val intent = when (nativePackages.contains(APPLICATION_ID)) {
                    true -> Intent(Intent.ACTION_VIEW, url.androidUri()).setPackage(APPLICATION_ID)
                    false -> Intent(Intent.ACTION_VIEW, url.androidUri()).apply {
                        if (!ProxerUrls.hasProxerHost(url)) {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                }

                activity.startActivity(intent.addReferer(activity))
            }
        }
    }
}

private fun CustomTabsHelperFragment.doOpenHttpPage(activity: Activity, url: HttpUrl) {
    CustomTabsIntent.Builder(session)
        .setToolbarColor(ContextCompat.getColor(activity, R.color.colorPrimary))
        .setSecondaryToolbarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark))
        .addDefaultShareMenuItem()
        .enableUrlBarHiding()
        .setShowTitle(true)
        .build()
        .let {
            it.intent.addReferer(activity)

            CustomTabsHelperFragment.open(activity, it, url.androidUri()) { context, uri ->
                WebViewActivity.navigateTo(context, uri.toString())
            }
        }
}
