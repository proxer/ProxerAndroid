package me.proxer.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.util.Patterns
import com.bumptech.glide.request.target.Target
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.LinkBuilder
import me.proxer.app.R
import me.proxer.app.application.GlideApp
import me.proxer.app.util.extension.androidUri
import me.proxer.library.api.ProxerException
import okhttp3.HttpUrl
import java.util.regex.Pattern

/**
 * @author Ruben Gees
 */
object Utils {

    const val GENERIC_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"

    private val WEB_REGEX = Patterns.WEB_URL
    private val MENTIONS_REGEX = Pattern.compile("(@[a-zA-Z0-9_]+)")

    fun setStatusBarColorIfPossible(activity: Activity?, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.apply {
                window?.statusBarColor = ContextCompat.getColor(activity, color)
            }
        }
    }

    fun getBitmapFromUrl(context: Context, url: String): Bitmap? {
        try {
            return GlideApp.with(context)
                    .asBitmap()
                    .load(url)
                    .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .get()
        } catch (ignored: Throwable) {
            return null
        }
    }

    fun buildClickableText(context: Context, text: CharSequence,
                           onWebClickListener: Link.OnClickListener? = null,
                           onWebLongClickListener: Link.OnLongClickListener? = null,
                           onMentionsClickListener: Link.OnClickListener? = null,
                           onMentionsLongClickListener: Link.OnLongClickListener? = null): CharSequence {
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

    fun parseAndFixUrl(url: String): HttpUrl {
        return HttpUrl.parse(when {
            url.startsWith("//") -> "http://$url"
            else -> url
        }) ?: throw ProxerException(ProxerException.ErrorType.PARSING)
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

    private fun extractPackageNames(resolveInfo: List<ResolveInfo>): MutableSet<String> {
        return resolveInfo
                .map { it.activityInfo.packageName }
                .toMutableSet()
    }
}
