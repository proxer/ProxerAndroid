@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.app.Activity
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.util.Linkify
import android.widget.ImageView
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.text.util.LinkifyCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import me.proxer.app.BuildConfig.APPLICATION_ID
import me.proxer.app.GlideRequest
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.settings.theme.ThemeVariant
import me.proxer.app.ui.LinkCheckDialog
import me.proxer.app.ui.WebViewActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.wrapper.SimpleGlideRequestListener
import me.proxer.library.ProxerException
import me.proxer.library.util.ProxerUrls.hasProxerHost
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.core.KoinComponent
import org.koin.core.parameter.DefinitionParameters
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
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

inline fun <T> unsafeLazy(noinline initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

inline fun Context.getQuantityString(id: Int, quantity: Int): String = resources
    .getQuantityString(id, quantity, quantity)

inline fun Fragment.dip(value: Int) = requireContext().dip(value)

inline fun CharSequence.linkify(web: Boolean = true, mentions: Boolean = true, vararg custom: Regex): Spannable {
    val spannable = this as? Spannable ?: SpannableString(this)

    if (web) LinkifyCompat.addLinks(spannable, Linkify.WEB_URLS)
    if (mentions) LinkifyCompat.addLinks(spannable, MENTIONS_REGEX, null)

    custom.forEach {
        LinkifyCompat.addLinks(spannable, it.toPattern(), null)
    }

    return spannable
}

fun PackageManager.isPackageInstalled(packageName: String) = try {
    getApplicationInfo(packageName, 0).enabled
} catch (error: PackageManager.NameNotFoundException) {
    false
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

fun String.toPrefixedUrlOrNull(): HttpUrl? = when {
    this.startsWith("http://") || this.startsWith("https://") -> this.toHttpUrlOrNull()
    else -> when {
        this.startsWith("//") -> "http:$this"
        else -> "http://$this"
    }.toHttpUrlOrNull()
}

fun String.toPrefixedHttpUrl() = toPrefixedUrlOrNull() ?: throw ProxerException(ProxerException.ErrorType.PARSING)

inline fun <T : Enum<T>> Bundle.putEnumSet(key: String, set: EnumSet<T>) {
    putIntArray(key, set.map { it.ordinal }.toIntArray())
}

inline fun <reified T : Enum<T>> Bundle.getEnumSet(key: String, klass: Class<T>): EnumSet<T> {
    val values = getIntArray(key)?.map { klass.enumConstants?.get(it) }?.filterNotNull()

    return when {
        values?.isEmpty() != false -> EnumSet.noneOf(T::class.java)
        else -> EnumSet.copyOf(values)
    }
}

inline fun Intent.addReferer(): Intent {
    putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://$APPLICATION_ID"))

    return this
}

// TODO: https://github.com/InsertKoinIO/koin/issues/303
@Suppress("UNCHECKED_CAST")
inline fun unsafeParametersOf(vararg parameters: Any?): DefinitionParameters {
    val constructor = DefinitionParameters::class.java.getConstructor(Array<Any?>::class.java)

    constructor.isAccessible = true

    return constructor.newInstance(parameters)
}

fun CustomTabsHelperFragment.handleLink(activity: FragmentActivity, url: HttpUrl, forceBrowser: Boolean = false) {
    if (forceBrowser) {
        openHttpPage(activity, url)
    } else {
        val nativePackages = Utils.getNativeAppPackage(activity, url)

        when (nativePackages.isEmpty()) {
            true -> {
                val preferenceHelper = activity.get<PreferenceHelper>()

                when (!url.hasProxerHost && preferenceHelper.shouldCheckLinks) {
                    true -> LinkCheckDialog.show(activity, url)
                    false -> openHttpPage(activity, url)
                }
            }
            false -> {
                val intent = when (nativePackages.contains(APPLICATION_ID)) {
                    true -> Intent(Intent.ACTION_VIEW, url.androidUri()).setPackage(APPLICATION_ID)
                    false -> Intent(Intent.ACTION_VIEW, url.androidUri()).apply {
                        if (!url.hasProxerHost) {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                }

                activity.startActivity(intent.addReferer())
            }
        }
    }
}

fun CustomTabsHelperFragment.openHttpPage(activity: Activity, url: HttpUrl) {
    val colorScheme = when (getKoin().get<PreferenceHelper>().themeContainer.variant) {
        ThemeVariant.LIGHT -> CustomTabsIntent.COLOR_SCHEME_LIGHT
        ThemeVariant.DARK -> CustomTabsIntent.COLOR_SCHEME_DARK
        ThemeVariant.SYSTEM -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
    }

    val colorSchemeParams = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(activity.resolveColor(R.attr.colorPrimary))
        .setSecondaryToolbarColor(activity.resolveColor(R.attr.colorPrimary))
        .setNavigationBarColor(activity.resolveColor(R.attr.colorPrimary))
        .build()

    CustomTabsIntent.Builder(session)
        .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, colorSchemeParams)
        .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, colorSchemeParams)
        .setColorScheme(colorScheme)
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

inline fun <reified T> KoinComponent.safeInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = lazy { getKoin().get(qualifier, parameters) }

inline fun <reified T> ComponentCallbacks.safeInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> = lazy { getKoin().get(qualifier, parameters) }
