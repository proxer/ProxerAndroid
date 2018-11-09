package me.proxer.app.newbase.paged

import android.os.Bundle
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import me.proxer.library.entity.ProxerIdItem

/**
 * @author Ruben Gees
 */
abstract class NewBasePagedListAdapter<T, VH : RecyclerView.ViewHolder>(
    itemCallback: DiffUtil.ItemCallback<T> = NewBaseItemCallback()
) : PagedListAdapter<T, VH>(itemCallback) {

    protected var positionResolver: (Int) -> Int = { it }
    protected var layoutManager: RecyclerView.LayoutManager? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        layoutManager = recyclerView.layoutManager
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        layoutManager = null
    }

    override fun getItemId(position: Int) = when (hasStableIds()) {
        true -> (getSafeItem(position) as ProxerIdItem).id.toLong()
        false -> super.getItemId(position)
    }

    fun initWithWrappingAdapter(adapter: EasyHeaderFooterAdapter) {
        positionResolver = { adapter.getRealPosition(it) }
    }

    open fun saveInstanceState(outState: Bundle) = Unit

    protected fun getSafeItem(position: Int): T {
        return getItem(position) ?: throw IllegalStateException("item at $position is null")
    }
}
