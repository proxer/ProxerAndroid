package com.proxerme.app.adapter.framework

import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.view.View
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class PagingAdapter<T>() : RecyclerView.Adapter<PagingAdapter.PagingViewHolder<T>>()
where T : Parcelable {

    protected val list: ArrayList<T> = arrayListOf()

    val items: List<T>
        get() {
            return ArrayList(list)
        }

    override fun onBindViewHolder(holder: PagingViewHolder<T>, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

    fun isEmpty() = list.isEmpty()

    open fun insert(items: Iterable<T>) {
        val filtered = items.filterNot { contains(it) }

        list.addAll(0, filtered)

        notifyItemRangeInserted(0, filtered.size)
    }

    fun insert(items: Array<T>) {
        insert(items.asIterable())
    }

    open fun append(items: Iterable<T>) {
        val filtered = items.filter { !contains(it) }
        val previousSize = list.size

        list.addAll(filtered)

        notifyItemRangeInserted(previousSize, filtered.size)
    }

    fun append(items: Array<T>) {
        append(items.asIterable())
    }

    open fun replace(items: Iterable<T>) {
        list.clear()
        list.addAll(items)

        notifyDataSetChanged()
    }

    fun replace(items: Array<T>) {
        replace(items.asIterable())
    }

    open fun remove(item: T) {
        val position = list.indexOf(item)

        list.removeAt(position)
        notifyItemRemoved(position)
    }

    open fun clear() {
        val previousSize = list.size

        list.clear()

        notifyItemRangeRemoved(0, previousSize)
    }

    open fun saveInstanceState(outState: Bundle) {

    }

    open fun contains(item: T): Boolean {
        return list.contains(item)
    }

    open fun removeCallback() {

    }

    abstract class PagingViewHolder<T>(itemView: View) :
            RecyclerView.ViewHolder(itemView) where T : Parcelable {

        open fun bind(item: T) {

        }

    }

}