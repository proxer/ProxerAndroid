package me.proxer.app.base

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import me.proxer.library.entity.ProxerIdItem

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    var positionResolver: PositionResolver = PositionResolver()

    protected var data = emptyList<T>()

    override fun getItemId(position: Int) = when (hasStableIds()) {
        true -> (data[position] as ProxerIdItem).id.toLong()
        false -> super.getItemId(position)
    }

    override fun getItemCount() = data.size

    open fun swapDataAndNotifyWithDiffing(newData: List<T>) {
        val diffResult = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun areItemsTheSame(old: Int, new: Int) = areItemsTheSame(data[old], newData[new])
                override fun areContentsTheSame(old: Int, new: Int) = areContentsTheSame(data[old], newData[new])
                override fun getOldListSize() = data.size
                override fun getNewListSize() = newData.size
            }
        )

        data = ArrayList(newData)

        diffResult.dispatchUpdatesTo(this)
    }

    open fun isEmpty() = data.isEmpty()

    open fun saveInstanceState(outState: Bundle) {}

    protected open fun areItemsTheSame(old: T, new: T) = when {
        old is ProxerIdItem && new is ProxerIdItem -> old.id == new.id
        else -> old == new
    }

    protected open fun areContentsTheSame(old: T, new: T) = old == new

    protected fun withSafeBindingAdapterPosition(holder: VH, action: (Int) -> Unit) =
        holder.bindingAdapterPosition.let {
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
