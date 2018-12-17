package me.proxer.app.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.bumptech.glide.request.target.Target
import me.proxer.app.GlideApp
import me.proxer.app.util.extension.androidUri
import me.proxer.library.api.ProxerException
import me.proxer.library.api.ProxerException.ErrorType
import okhttp3.HttpUrl
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber

/**
 * @author Ruben Gees
 */
object Utils {

    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun setStatusBarColorIfPossible(activity: Activity?, @ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.apply {
                window?.statusBarColor = ContextCompat.getColor(activity, color)
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
        Timber.e(error)

        null
    }

    fun parseAndFixUrl(url: String) = when {
        url.startsWith("http://") || url.startsWith("https://") -> HttpUrl.parse(url)
        else -> HttpUrl.parse(
            when {
                url.startsWith("//") -> "http:$url"
                else -> "http://$url"
            }
        )
    }

    fun getAndFixUrl(url: String) = parseAndFixUrl(url) ?: throw ProxerException(ErrorType.PARSING)

    fun isPackageInstalled(packageManager: PackageManager, packageName: String) = try {
        packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (error: PackageManager.NameNotFoundException) {
        false
    }

    fun getNativeAppPackage(context: Context, url: HttpUrl): Set<String> {
        val browserActivityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.generic.com"))
        val genericResolvedList = extractPackageNames(
            context.packageManager.queryIntentActivities(browserActivityIntent, 0)
        )

        val specializedActivityIntent = Intent(Intent.ACTION_VIEW, url.androidUri())
        val resolvedSpecializedList = extractPackageNames(
            context.packageManager.queryIntentActivities(specializedActivityIntent, 0)
        )

        resolvedSpecializedList.removeAll(genericResolvedList)

        return resolvedSpecializedList
    }

    private fun extractPackageNames(resolveInfo: List<ResolveInfo>) = resolveInfo
        .asSequence()
        .map { it.activityInfo.packageName }
        .toMutableSet()
}
