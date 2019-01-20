package me.proxer.app.manga

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.proxer.app.util.DeviceUtils

/**
 * @author Ruben Gees
 */
class MangaLinearLayoutManger(
    context: Context,
    isVertical: Boolean
) : LinearLayoutManager(context, if (isVertical) LinearLayoutManager.VERTICAL else HORIZONTAL, false) {

    private val extraLayoutSpace = when {
        isVertical -> DeviceUtils.getScreenHeight(context) / 2
        else -> DeviceUtils.getScreenWidth(context) / 2
    }

    init {
        isItemPrefetchEnabled = false
    }

    override fun getExtraLayoutSpace(state: RecyclerView.State) = extraLayoutSpace
}
