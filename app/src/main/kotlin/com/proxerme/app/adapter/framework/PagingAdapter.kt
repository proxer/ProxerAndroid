package com.proxerme.app.adapter.framework

import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.RecyclerView
import android.view.View
import com.proxerme.library.interfaces.IdItem
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class PagingAdapter<T, C : PagingAdapter.PagingAdapterCallback<T>>() :
        RecyclerView.Adapter<PagingAdapter.PagingViewHolder<T, C>>() where T : Parcelable, T : IdItem {

    open protected val hasStableIds = true

    protected val list: ArrayList<T> = arrayListOf()
    var callback: C? = null

    init {
        setHasStableIds(hasStableIds)
    }

    override fun onBindViewHolder(holder: PagingViewHolder<T, C>, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

    override fun getItemId(position: Int) = list[position].id.toLong()

    fun isEmpty() = list.isEmpty()

    open fun insert(items: Iterable<T>) {
        val filtered = items.filter { !contains(it) }

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

    abstract class PagingViewHolder<T, out C : PagingAdapter.PagingAdapterCallback<T>>(itemView: View) :
            RecyclerView.ViewHolder(itemView) where T : Parcelable, T : IdItem {

        abstract val adapterList: List<T>
        abstract val adapterCallback: C?

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    adapterCallback?.onItemClick(it, adapterList[adapterPosition])
                }
            }
        }

        open fun bind(item: T) {

        }

    }

    abstract class PagingAdapterCallback<T> {
        open fun onItemClick(v: View, item: T) {

        }
    }

}