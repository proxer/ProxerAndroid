package me.proxer.app.adapter.base

import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import me.proxer.library.entitiy.ProxerIdItem

/**
 * @author Ruben Gees
 */
abstract class BaseAdapter<T> : RecyclerView.Adapter<BaseAdapter<T>.BaseViewHolder<T>>() {

    val list: List<T> get() = ArrayList(internalList)

    var positionResolver = PositionResolver()
    var recyclerView: RecyclerView? = null

    protected val internalList = ArrayList<T>()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        this.recyclerView = null

        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        holder.bind(internalList[position])
    }

    override fun getItemId(position: Int) = if (hasStableIds()) {
        (internalList[position] as ProxerIdItem).id.toLong()
    } else {
        super.getItemId(position)
    }

    override fun getItemCount() = internalList.size

    fun isEmpty() = internalList.isEmpty()

    open fun insert(items: Iterable<T>) {
        doUpdates(items.plus(internalList.filter { oldItem ->
            items.find { areItemsTheSame(oldItem, it) } == null
        }))
    }

    open fun append(items: Iterable<T>) {
        doUpdates(internalList.filter { oldItem ->
            items.find { areItemsTheSame(oldItem, it) } == null
        }.plus(items))
    }

    open fun replace(items: Iterable<T>) {
        doUpdates(items.toList())
    }

    open fun remove(item: T) {
        doUpdates(internalList.minus(item))
    }

    open fun remove(id: Long) {
        doUpdates(internalList.filterIndexed { index, _ -> getItemId(index) != id })
    }

    open fun clear() {
        internalList.clear()

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
        val wasEmpty = internalList.isEmpty()
        val wasAtFirstPosition = isAtFirstPosition()

        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int)
                    = areItemsTheSame(internalList[oldItemPosition], newList[newItemPosition])

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int)
                    = areContentsTheSame(internalList[oldItemPosition], newList[newItemPosition])

            override fun getOldListSize() = internalList.size
            override fun getNewListSize() = newList.size
        })

        internalList.clear()
        internalList.addAll(newList)

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
        }
    }

    private fun isAtFirstPosition(): Boolean {
        val safeLayoutManager = recyclerView?.layoutManager

        return when (safeLayoutManager) {
            is StaggeredGridLayoutManager -> safeLayoutManager.findFirstVisibleItemPositions(null).contains(0)
            is LinearLayoutManager -> safeLayoutManager.findFirstVisibleItemPosition() == 0
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

    inner abstract class BaseViewHolder<in T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

        open fun bind(item: T) {}

        internal fun withSafeAdapterPosition(action: (Int) -> Unit) {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                action.invoke(positionResolver.resolveRealPosition(adapterPosition))
            }
        }
    }

    open class PositionResolver {
        open fun resolveRealPosition(position: Int) = position
    }
}
