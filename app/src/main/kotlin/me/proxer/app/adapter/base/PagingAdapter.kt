package me.proxer.app.adapter.base

import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import me.proxer.library.entitiy.ProxerIdItem
import java.util.*

/**
 * @author Ruben Gees
 */
abstract class PagingAdapter<T> : RecyclerView.Adapter<PagingAdapter<T>.PagingViewHolder<T>>() {

    var positionResolver = PositionResolver()

    var list = ArrayList<T>()
        protected set

    var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        this.recyclerView = null

        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onBindViewHolder(holder: PagingViewHolder<T>, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemId(position: Int): Long {
        if (hasStableIds()) {
            return (list[position] as ProxerIdItem).id.toLong()
        } else {
            return super.getItemId(position)
        }
    }

    override fun getItemCount() = list.size

    fun isEmpty() = list.isEmpty()

    open fun insert(items: Iterable<T>) {
        doUpdates(items.plus(list.filter { oldItem ->
            items.find { areItemsTheSame(oldItem, it) } == null
        }))
    }

    open fun append(items: Iterable<T>) {
        doUpdates(list.filter { oldItem ->
            items.find { areItemsTheSame(oldItem, it) } == null
        }.plus(items))
    }

    open fun replace(items: Iterable<T>) {
        doUpdates(items.toList())
    }

    open fun remove(item: T) {
        doUpdates(list.minus(item))
    }

    open fun clear() {
        list.clear()

        notifyDataSetChanged()
    }

    open protected fun areItemsTheSame(oldItem: T, newItem: T) = when {
        hasStableIds() -> (oldItem as ProxerIdItem).id == (newItem as ProxerIdItem).id
        else -> oldItem == newItem
    }

    open protected fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem

    open fun destroy() {
        positionResolver = PositionResolver()
    }

    protected fun doUpdates(newList: List<T>) {
        val wasEmpty = list.isEmpty()
        val wasAtFirstPosition = isAtFirstPosition()

        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int)
                    = areItemsTheSame(list[oldItemPosition], newList[newItemPosition])

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int)
                    = areContentsTheSame(list[oldItemPosition], newList[newItemPosition])

            override fun getOldListSize() = list.size
            override fun getNewListSize() = newList.size
        })

        list.clear()
        list.addAll(newList)

        result.dispatchUpdatesTo(this)

        if (newList.isNotEmpty()) {
            if (wasEmpty || wasAtFirstPosition) {
                recyclerView?.postDelayed({
                    when {
                        wasEmpty -> scrollToTop()
                        wasAtFirstPosition -> recyclerView?.smoothScrollToPosition(0)
                    }
                }, 50)
            }

            recyclerView?.postDelayed({
                recyclerView?.invalidateItemDecorations()
            }, 500)
        }
    }

    private fun isAtFirstPosition(): Boolean {
        val safeLayoutManager = recyclerView?.layoutManager

        return when (safeLayoutManager) {
            is StaggeredGridLayoutManager -> safeLayoutManager.findFirstCompletelyVisibleItemPositions(null).contains(0)
            is LinearLayoutManager -> safeLayoutManager.findFirstCompletelyVisibleItemPosition() == 0
            else -> false
        }
    }

    private fun scrollToTop() {
        val safeLayoutManager = recyclerView?.layoutManager

        when (safeLayoutManager) {
            is StaggeredGridLayoutManager -> safeLayoutManager.scrollToPositionWithOffset(0, 0)
            is LinearLayoutManager -> safeLayoutManager.scrollToPositionWithOffset(0, 0)
        }
    }

    inner abstract class PagingViewHolder<in T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

        open fun bind(item: T) {

        }

        protected fun withSafeAdapterPosition(action: (Int) -> Unit) {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                action.invoke(positionResolver.resolveRealPosition(adapterPosition))
            }
        }
    }

    open class PositionResolver {
        open fun resolveRealPosition(position: Int) = position
    }
}
