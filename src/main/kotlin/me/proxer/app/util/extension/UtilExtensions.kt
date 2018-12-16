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
import android.widget.ImageView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.text.util.LinkifyCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import me.proxer.app.BuildConfig
import me.proxer.app.BuildConfig.APPLICATION_ID
import me.proxer.app.GlideRequest
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.ui.WebViewActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.wrapper.SimpleGlideRequestListener
import me.proxer.library.util.ProxerUrls
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import timber.log.Timber
import java.util.EnumSet
import java.util.regex.Pattern.quote

val MENTIONS_REGEX = Regex("@(?:.*?)(?:(?:(?! )(?!${quote(".")} )(?!${quote(".")}\n)(?!\n)).)*").toPattern()

inline fun <reified T : Enum<T>> enumSetOf(collection: Collection<T>): EnumSet<T> = when (collection.isEmpty()) {
    true -> EnumSet.noneOf(T::class.java)
    false -> EnumSet.copyOf(collection)
}

inline fun <reified T : Enum<T>> enumSetOf(vararg items: T): EnumSet<T> = when (items.isEmpty()) {
    true -> EnumSet.noneOf(T::class.java)
    false -> EnumSet.copyOf(items.toSet())
}

inline fun <K : Any, V : Any> Map<K?, V>.filterKeysNotNull(): Map<K, V> {
    val destination = HashMap<K, V>()

    for (element in this) {
        val (key, value) = element

        if (key != null) destination[key] = value
    }

    return destination
}

inline fun <T> unsafeLazy(noinline initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

inline fun Context.getQuantityString(id: Int, quantity: Int): String = resources
    .getQuantityString(id, quantity, quantity)

inline fun Fragment.dip(value: Int) = context?.dip(value) ?: throw IllegalStateException("context is null")

inline fun CharSequence.linkify(web: Boolean = true, mentions: Boolean = true, vararg custom: Regex): Spannable {
    val spannable = this as? Spannable ?: SpannableString(this)

    if (web) LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS)
    if (mentions) LinkifyCompat.addLinks(spannable, MENTIONS_REGEX, null)

    custom.forEach {
        LinkifyCompat.addLinks(spannable, it.toPattern(), "")
    }

    return spannable
}

inline fun GlideRequests.defaultLoad(view: ImageView, url: HttpUrl): Target<Drawable> = load(url.toString())
    .transition(DrawableTransitionOptions.withCrossFade())
    .logErrors()
    .into(view)

inline fun <T> GlideRequest<T>.logErrors(): GlideRequest<T> = this.addListener(object : SimpleGlideRequestListener<T> {
    override fun onLoadFailed(error: GlideException?): Boolean {
        if (error != null) Timber.e(error)

        return false
    }
})

inline fun HttpUrl.androidUri(): Uri = Uri.parse(toString())

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

inline fun Intent.addReferer(): Intent {
    val referrerExtraName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        Intent.EXTRA_REFERRER
    } else {
        "android.intent.extra.REFERRER"
    }

    putExtra(referrerExtraName, Uri.parse("android-app://" + BuildConfig.APPLICATION_ID))

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

                activity.startActivity(intent.addReferer())
            }
        }
    }
}

private fun CustomTabsHelperFragment.doOpenHttpPage(activity: Activity, url: HttpUrl) {
    CustomTabsIntent.Builder(session)
        .setToolbarColor(ContextCompat.getColor(activity, R.color.primaryColor))
        .setSecondaryToolbarColor(ContextCompat.getColor(activity, R.color.primaryDarkColor))
        .addDefaultShareMenuItem()
        .enableUrlBarHiding()
        .setShowTitle(true)
        .build()
        .let {
            it.intent.addReferer()

            CustomTabsHelperFragment.open(activity, it, url.androidUri()) { context, uri ->
                WebViewActivity.navigateTo(context, uri.toString())
            }
        }
}
