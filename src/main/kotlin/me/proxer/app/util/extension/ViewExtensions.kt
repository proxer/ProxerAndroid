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

fun RecyclerView.isAtCompleteTop() = when (val layoutManager = this.safeLayoutManager) {
    is StaggeredGridLayoutManager -> layoutManager.findFirstCompletelyVisibleItemPositions(null).contains(0)
    is LinearLayoutManager -> layoutManager.findFirstCompletelyVisibleItemPosition() == 0
    else -> false
}

fun RecyclerView.isAtTop() = when (val layoutManager = this.safeLayoutManager) {
    is StaggeredGridLayoutManager -> layoutManager.findFirstVisibleItemPositions(null).contains(0)
    is LinearLayoutManager -> layoutManager.findFirstVisibleItemPosition() == 0
    else -> false
}

fun RecyclerView.scrollToTop() = when (val layoutManager = safeLayoutManager) {
    is StaggeredGridLayoutManager -> layoutManager.scrollToPositionWithOffset(0, 0)
    is LinearLayoutManager -> layoutManager.scrollToPositionWithOffset(0, 0)
    else -> throw IllegalStateException("Unsupported layout manager: ${layoutManager.javaClass.name}")
}

fun RecyclerView.doAfterAnimations(action: () -> Unit) {
    post {
        if (isAnimating) {
            val safeItemAnimator = itemAnimator ?: throw IllegalStateException(
                "RecyclerView is reporting" +
                    "isAnimating as true, but no itemAnimator is set"
            )

            safeItemAnimator.isRunning { doAfterAnimations(action) }
        } else {
            action()
        }
    }
}
