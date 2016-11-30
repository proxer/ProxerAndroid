package com.proxerme.app.adapter.framework

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.View
import com.proxerme.library.interfaces.IdItem
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class PagingAdapter<T>() : RecyclerView.Adapter<PagingAdapter.PagingViewHolder<T>>() {

    protected var list: ArrayList<T> = ArrayList<T>()

    val items: List<T>
        get() = ArrayList(list)

    override fun onBindViewHolder(holder: PagingViewHolder<T>, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size
    fun isEmpty() = list.isEmpty()

    fun insert(items: Iterable<T>) {
        doUpdates(items.plus(ArrayList<T>(list).apply {
            removeAll<T> { oldItem ->
                if (hasStableIds()) {
                    items.find { newItem ->
                        (oldItem as IdItem).id == (newItem as IdItem).id
                    } != null
                } else {
                    items.contains(oldItem)
                }
            }
        }))
    }

    fun insert(items: Array<T>) {
        insert(items.asIterable())
    }

    fun append(items: Iterable<T>) {
        doUpdates(ArrayList<T>(list).apply {
            removeAll<T> { oldItem ->
                if (hasStableIds()) {
                    items.find { newItem ->
                        (oldItem as IdItem).id == (newItem as IdItem).id
                    } != null
                } else {
                    items.contains(oldItem)
                }
            }
        }.plus(items))
    }

    fun append(items: Array<T>) {
        append(items.asIterable())
    }

    fun replace(items: Iterable<T>) {
        doUpdates(items.toList())
    }

    fun replace(items: Array<T>) {
        doUpdates(items.toList())
    }

    open fun remove(item: T) {
        doUpdates(list.minus(item))
    }

    open fun clear() {
        doUpdates(ArrayList<T>(0))
    }

    private fun doUpdates(newList: List<T>) {
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (hasStableIds()) {
                    return (list[oldItemPosition] as IdItem).id ==
                            (newList[newItemPosition] as IdItem).id
                } else {
                    return list[oldItemPosition] == newList[newItemPosition]
                }
            }

            override fun getOldListSize(): Int {
                return list.size
            }

            override fun getNewListSize(): Int {
                return newList.size
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return list[oldItemPosition] == newList[newItemPosition]
            }
        }, true)

        list.clear()
        list.addAll(newList)
        result.dispatchUpdatesTo(this)
    }

    open fun removeCallback() {

    }

    abstract class PagingViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

        open fun bind(item: T) {

        }

    }

}