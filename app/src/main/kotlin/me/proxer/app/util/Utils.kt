package me.proxer.app.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.util.Patterns
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.LinkBuilder
import me.proxer.app.R
import me.proxer.app.util.extension.androidUri
import okhttp3.HttpUrl
import java.util.regex.Pattern

/**
 * Class which holds various util methods.

 * @author Ruben Gees
 */
object Utils {

    private val WEB_REGEX = Patterns.WEB_URL
    private val MENTIONS_REGEX = Pattern.compile("(@[a-zA-Z0-9_]+)")

    fun setStatusBarColorIfPossible(activity: Activity?, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.apply {
                window?.statusBarColor = ContextCompat.getColor(activity, color)
            }
        }
    }

    fun getBitmapFromURL(context: Context, url: String): Bitmap? {
        try {
            return Glide.with(context)
                    .load(url)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
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

    fun setClipboardContent(activity: Activity, label: String, content: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager
        val clip = ClipData.newPlainText(label, content)

        clipboard.primaryClip = clip
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

//    fun getLanguages(vararg items: String): List<Language> {
//        return items.map {
//            when (it) {
//                SubDubLanguageParameter.ENGLISH_SUB, SubDubLanguageParameter.ENGLISH_DUB,
//                GeneralLanguageParameter.ENGLISH -> ENGLISH
//                SubDubLanguageParameter.GERMAN_SUB, SubDubLanguageParameter.GERMAN_DUB,
//                GeneralLanguageParameter.GERMAN -> GERMAN
//                else -> null
//            }
//        }.filterNotNull()
//    }

    private fun extractPackageNames(resolveInfo: List<ResolveInfo>): MutableSet<String> {
        return resolveInfo
                .map { it.activityInfo.packageName }
                .toMutableSet()
    }

    enum class Language { ENGLISH, GERMAN }
}
