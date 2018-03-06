@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import me.proxer.app.BuildConfig.APPLICATION_ID
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.ui.WebViewActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.Utils
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import org.jetbrains.anko.dip
import java.util.EnumSet

inline fun <reified T : Enum<T>> enumSetOf(collection: Collection<T>): EnumSet<T> = when (collection.isEmpty()) {
    true -> EnumSet.noneOf(T::class.java)
    false -> EnumSet.copyOf(collection)
}

inline fun <T> unsafeLazy(noinline initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

inline fun Context.getDrawableFromAttrs(resource: Int): Drawable {
    val styledAttributes = obtainStyledAttributes(intArrayOf(resource))
    val result = styledAttributes.getDrawable(0)

    styledAttributes.recycle()

    return result
}

inline fun Context.getQuantityString(id: Int, quantity: Int): String = resources
        .getQuantityString(id, quantity, quantity)

inline fun Fragment.dip(value: Int) = context?.dip(value) ?: throw IllegalStateException("context is null")

inline fun IconicsDrawable.colorRes(context: Context, id: Int): IconicsDrawable {
    return this.color(ContextCompat.getColor(context, id))
}

inline fun IconicsDrawable.iconColor(context: Context): IconicsDrawable {
    return this.colorRes(context, R.color.icon)
}

inline fun ImageView.setIconicsImage(
    icon: IIcon,
    sizeDp: Int,
    paddingDp: Int = sizeDp / 4,
    colorRes: Int = R.color.icon
) {
    setImageDrawable(IconicsDrawable(context, icon)
            .sizeDp(sizeDp)
            .paddingDp(paddingDp)
            .colorRes(context, colorRes))
}

inline fun GlideRequests.defaultLoad(view: ImageView, url: HttpUrl): Target<Drawable> = load(url.toString())
        .transition(DrawableTransitionOptions.withCrossFade())
        .format(when (DeviceUtils.shouldShowHighQualityImages(view.context)) {
            true -> DecodeFormat.PREFER_ARGB_8888
            false -> DecodeFormat.PREFER_RGB_565
        })
        .into(view)

inline fun HttpUrl.androidUri(): Uri = Uri.parse(toString())

inline fun <T : View> T.postDelayedSafely(crossinline callback: (T) -> Unit, delayMillis: Long) {
    postDelayed({ callback(this) }, delayMillis)
}

inline fun ViewGroup.enableLayoutAnimationsSafely() {
    this.layoutTransition = LayoutTransition().apply { setAnimateParentHierarchy(false) }
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

fun CustomTabsHelperFragment.openHttpPage(activity: Activity, url: HttpUrl, forceBrowser: Boolean = false) {
    if (forceBrowser) {
        doOpenHttpPage(activity, url)
    } else {
        val nativePackages = Utils.getNativeAppPackage(activity, url)

        when (nativePackages.isEmpty()) {
            true -> doOpenHttpPage(activity, url)
            false -> when (nativePackages.contains(APPLICATION_ID)) {
                true -> activity.startActivity(Intent(Intent.ACTION_VIEW, url.androidUri()).setPackage(APPLICATION_ID))
                false -> activity.startActivity(Intent(Intent.ACTION_VIEW, url.androidUri()))
            }
        }
    }
}

fun RecyclerView.LayoutManager.isAtCompleteTop() = when (this) {
    is StaggeredGridLayoutManager -> findFirstCompletelyVisibleItemPositions(null).contains(0)
    is LinearLayoutManager -> findFirstCompletelyVisibleItemPosition() == 0
    else -> false
}

fun RecyclerView.LayoutManager.isAtTop() = when (this) {
    is StaggeredGridLayoutManager -> findFirstVisibleItemPositions(null).contains(0)
    is LinearLayoutManager -> findFirstVisibleItemPosition() == 0
    else -> false
}

fun RecyclerView.LayoutManager.scrollToTop() = when (this) {
    is StaggeredGridLayoutManager -> scrollToPositionWithOffset(0, 0)
    is LinearLayoutManager -> scrollToPositionWithOffset(0, 0)
    else -> Unit
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
                CustomTabsHelperFragment.open(activity, it, url.androidUri(), { context, uri ->
                    WebViewActivity.navigateTo(context, uri.toString())
                })
            }
}
