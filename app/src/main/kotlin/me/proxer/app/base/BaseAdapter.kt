package me.proxer.app.base

import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import me.proxer.library.entitiy.ProxerIdItem

/**
 * @author Ruben Gees
 */
abstract class BaseAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    var positionResolver: PositionResolver = PositionResolver()

    protected var data = emptyList<T>()

    override fun getItemId(position: Int) = when (hasStableIds()) {
        true -> (data[position] as ProxerIdItem).id.toLong()
        false -> super.getItemId(position)
    }

    override fun getItemCount() = data.size
    open fun isEmpty() = data.isEmpty()

    open fun clear() {
        data = emptyList()
    }

    fun clearAndNotifyRemoval() = itemCount.let {
        clear()

        notifyItemRangeRemoved(0, it)
    }

    fun clearAndNotifyChange() {
        clear()

        notifyDataSetChanged()
    }

    open fun swapData(newData: List<T>) {
        data = ArrayList(newData)
    }

    fun swapDataAndNotifyInsertion(newData: List<T>) {
        swapData(newData)

        notifyItemRangeInserted(0, newData.size)
    }

    fun swapDataAndNotifyChange(newData: List<T>) {
        swapData(newData)

        notifyDataSetChanged()
    }

    fun swapDataAndNotifyWithDiffResult(newData: List<T>, diffResult: DiffUtil.DiffResult) {
        swapData(newData)

        diffResult.dispatchUpdatesTo(this)
    }

    fun provideDiffUtilCallback(newData: List<T>) = object : DiffUtil.Callback() {
        override fun areItemsTheSame(old: Int, new: Int) = areItemsTheSame(data[old], newData[new])
        override fun areContentsTheSame(old: Int, new: Int) = areContentsTheSame(data[old], newData[new])
        override fun getOldListSize() = data.size
        override fun getNewListSize() = newData.size
    }

    open fun saveInstanceState(outState: Bundle) = Unit

    open protected fun areItemsTheSame(old: T, new: T) = when {
        old is ProxerIdItem && new is ProxerIdItem -> old.id == new.id
        else -> old == new
    }

    open protected fun areContentsTheSame(old: T, new: T) = old == new

    protected fun withSafeAdapterPosition(holder: VH, action: (Int) -> Unit) = holder.adapterPosition.let {
        if (it != RecyclerView.NO_POSITION) {
            action(positionResolver.resolve(it))
        }
    }

    open class PositionResolver {
        open fun resolve(position: Int) = position
    }

    class ContainerPositionResolver(private val adapterContainer: EasyHeaderFooterAdapter) : PositionResolver() {
        override fun resolve(position: Int) = adapterContainer.getRealPosition(position)
    }
}
