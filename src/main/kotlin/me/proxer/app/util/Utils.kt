package me.proxer.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.Patterns
import com.bumptech.glide.request.target.Target
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.LinkBuilder
import me.proxer.app.GlideApp
import me.proxer.app.MainApplication.Companion.LOGGING_TAG
import me.proxer.app.R
import me.proxer.app.util.extension.androidUri
import me.proxer.library.api.ProxerException
import me.proxer.library.api.ProxerException.ErrorType
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import org.jetbrains.anko.getStackTraceString
import org.threeten.bp.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * @author Ruben Gees
 */
object Utils {

    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")

    private val WEB_REGEX = Patterns.WEB_URL
    private val MENTIONS_REGEX = Pattern.compile("(@[a-zA-Z0-9_-]+)")

    fun setStatusBarColorIfPossible(activity: Activity?, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.apply {
                window?.statusBarColor = ContextCompat.getColor(activity, color)
            }
        }
    }

    fun setNavigationBarColorIfPossible(activity: Activity?, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.apply {
                window?.navigationBarColor = ContextCompat.getColor(activity, color)
            }
        }
    }

    fun getCircleBitmapFromUrl(context: Context, url: HttpUrl) = try {
        GlideApp.with(context)
                .asBitmap()
                .load(url.toString())
                .circleCrop()
                .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .get()
    } catch (error: Throwable) {
        Log.e(LOGGING_TAG, error.getStackTraceString())

        null
    }

    fun buildClickableText(
            context: Context, text: CharSequence,
            onWebClickListener: Link.OnClickListener? = null,
            onWebLongClickListener: Link.OnLongClickListener? = null,
            onMentionsClickListener: Link.OnClickListener? = null,
            onMentionsLongClickListener: Link.OnLongClickListener? = null
    ): CharSequence {
        val builder = LinkBuilder.from(context, text.toString())

        if (onWebClickListener != null || onWebLongClickListener != null) {
            builder.addLink(Link(WEB_REGEX)
                    .setTextColor(ContextCompat.getColor(context, R.color.link))
                    .setUnderlined(false)
                    .setOnClickListener(onWebClickListener)
                    .setOnLongClickListener(onWebLongClickListener))
        }

        if (onMentionsClickListener != null || onMentionsLongClickListener != null) {
            builder.addLink(Link(MENTIONS_REGEX)
                    .setTextColor(ContextCompat.getColor(context, R.color.link))
                    .setUnderlined(false)
                    .setOnClickListener(onMentionsClickListener)
                    .setOnLongClickListener(onMentionsLongClickListener))
        }

        var result = builder.build()

        if (result == null) {
            result = text
        }

        return result
    }

    fun safelyParseAndFixUrl(url: String): HttpUrl? {
        val fixedUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> HttpUrl.parse(url)
            else -> HttpUrl.parse(when {
                url.startsWith("//") -> "http:$url"
                else -> "http://$url"
            })
        }

        return fixedUrl?.let {
            when (isEligibleForHttps(it)) {
                true -> it.newBuilder().scheme("https").build()
                false -> it
            }
        }
    }

    fun parseAndFixUrl(url: String) = safelyParseAndFixUrl(url)
            ?: throw ProxerException(ErrorType.PARSING)

    fun isPackageInstalled(packageManager: PackageManager, packageName: String) = try {
        packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (error: PackageManager.NameNotFoundException) {
        false
    }

    fun getNativeAppPackage(context: Context, url: HttpUrl): Set<String> {
        val browserActivityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.generic.com"))
        val genericResolvedList = extractPackageNames(context.packageManager
                .queryIntentActivities(browserActivityIntent, 0))

        val specializedActivityIntent = Intent(Intent.ACTION_VIEW, url.androidUri())
        val resolvedSpecializedList = extractPackageNames(context.packageManager
                .queryIntentActivities(specializedActivityIntent, 0))

        resolvedSpecializedList.removeAll(genericResolvedList)

        return resolvedSpecializedList
    }

    private fun isEligibleForHttps(url: HttpUrl) = when {
        ProxerUrls.hasProxerHost(url) -> true
        else -> false
    }

    private fun extractPackageNames(resolveInfo: List<ResolveInfo>) = resolveInfo
            .map { it.activityInfo.packageName }
            .toMutableSet()
}
