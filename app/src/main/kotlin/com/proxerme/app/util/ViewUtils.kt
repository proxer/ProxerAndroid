package com.proxerme.app.util

import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import cn.nekocode.badge.BadgeDrawable
import com.proxerme.app.R

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
object ViewUtils {

    fun <T> populateBadgeView(badgeContainer: ViewGroup, items: Array<T>,
                              transform: (T) -> String,
                              onClick: ((View, T) -> Unit)? = null,
                              textSizeSp: Float = 14f) {
        items.forEach { item ->
            badgeContainer.addView(buildBadgeViewEntry(badgeContainer, item, transform, onClick,
                    textSizeSp))
        }
    }

    fun <T> buildBadgeViewEntry(container: ViewGroup, item: T, transform: (T) -> String,
                                onClick: ((View, T) -> Unit)? = null,
                                textSizeSp: Float = 14f, imageViewToReuse: ImageView? = null):
            ImageView {
        val imageView = imageViewToReuse ?: LayoutInflater.from(container.context)
                .inflate(R.layout.item_badge, container, false) as ImageView

        imageView.setImageDrawable(BadgeDrawable.Builder()
                .type(BadgeDrawable.TYPE_ONLY_ONE_TEXT)
                .badgeColor(ContextCompat.getColor(container.context,
                        R.color.colorAccent))
                .text1(transform.invoke(item))
                .textSize(DeviceUtils.convertSpToPx(container.context, textSizeSp))
                .build()
                .apply {
                    setNeedAutoSetBounds(true)
                })

        if (onClick != null) {
            imageView.setOnClickListener {
                onClick.invoke(it, item)
            }
        }

        return imageView
    }
}
