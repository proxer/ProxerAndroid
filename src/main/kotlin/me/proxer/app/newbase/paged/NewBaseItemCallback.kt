package me.proxer.app.newbase.paged

import androidx.recyclerview.widget.DiffUtil
import me.proxer.library.entity.ProxerIdItem

/**
 * @author Ruben Gees
 */
class NewBaseItemCallback<T> : DiffUtil.ItemCallback<T>() {

    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return if (oldItem is ProxerIdItem && newItem is ProxerIdItem) {
            oldItem.id == newItem.id
        } else {
            oldItem == newItem
        }
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem == newItem
    }
}
