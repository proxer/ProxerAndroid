@file:Suppress("NOTHING_TO_INLINE")

package me.proxer.app.util.extension

import android.animation.LayoutTransition
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import androidx.core.view.postDelayed
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import me.proxer.app.R
import timber.log.Timber

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
    @AttrRes colorAttr: Int = R.attr.colorIcon
) {
    setImageDrawable(
        IconicsDrawable(context, icon).apply {
            this.colorInt = context.resolveColor(colorAttr)
            this.paddingDp = paddingDp
            this.sizeDp = sizeDp
        }
    )
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
    else -> error("Unsupported layout manager: ${layoutManager.javaClass.name}")
}

fun RecyclerView.doAfterAnimations(action: () -> Unit) {
    postDelayed(10) {
        if (isAnimating) {
            val safeItemAnimator = requireNotNull(itemAnimator) {
                "RecyclerView is reporting isAnimating as true, but no itemAnimator is set"
            }

            safeItemAnimator.isRunning { doAfterAnimations(action) }
        } else {
            action()
        }
    }
}

fun RecyclerView.enableFastScroll() {
    val thumbColor = context.resolveColor(R.attr.colorFastscrollThumb)
    val trackColor = context.resolveColor(R.attr.colorFastscrollTrack)
    val pressedColor = context.resolveColor(R.attr.colorSecondary)

    val thumbDrawableSelector = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(pressedColor))
        addState(intArrayOf(), ColorDrawable(thumbColor))
    }

    val trackDrawable = ColorDrawable(trackColor)

    try {
        val initFastScrollerMethod = RecyclerView::class.java.getDeclaredMethod(
            "initFastScroller",
            StateListDrawable::class.java,
            Drawable::class.java,
            StateListDrawable::class.java,
            Drawable::class.java
        ).apply {
            isAccessible = true
        }

        initFastScrollerMethod.invoke(
            this,
            thumbDrawableSelector,
            trackDrawable,
            thumbDrawableSelector,
            trackDrawable
        )
    } catch (error: Exception) {
        Timber.e(error, "Could not enable fast scroll")
    }
}
