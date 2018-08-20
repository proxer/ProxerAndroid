@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.animation.LayoutTransition
import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import me.proxer.app.R

inline var AppCompatTextView.fastText: CharSequence
    get() = text
    set(value) {
        val textMetrics = TextViewCompat.getTextMetricsParams(this)

        setTextFuture(PrecomputedTextCompat.getTextFuture(value, textMetrics, null))
    }

inline fun ImageView.setIconicsImage(
    icon: IIcon,
    sizeDp: Int,
    paddingDp: Int = sizeDp / 4,
    colorRes: Int = R.color.icon
) {
    setImageDrawable(
        IconicsDrawable(context, icon)
            .sizeDp(sizeDp)
            .paddingDp(paddingDp)
            .colorRes(context, colorRes)
    )
}

inline fun IconicsDrawable.colorRes(context: Context, id: Int): IconicsDrawable {
    return this.color(ContextCompat.getColor(context, id))
}

inline fun IconicsDrawable.iconColor(context: Context): IconicsDrawable {
    return this.colorRes(context, R.color.icon)
}

inline fun ViewGroup.enableLayoutAnimationsSafely() {
    this.layoutTransition = LayoutTransition().apply { setAnimateParentHierarchy(false) }
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
