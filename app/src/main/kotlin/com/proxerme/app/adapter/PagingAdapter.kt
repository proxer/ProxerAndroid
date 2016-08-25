package com.proxerme.app.adapter

import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.CallSuper
import android.support.v7.widget.RecyclerView
import android.view.View
import com.proxerme.library.interfaces.IdItem
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class PagingAdapter<T>(savedInstanceState: Bundle?) :
        RecyclerView.Adapter<PagingAdapter.PagingViewHolder<T>>() where T : Parcelable, T : IdItem {

    abstract protected val stateKey: String

    protected val list: ArrayList<T> = arrayListOf()

    init {
        setHasStableIds(true)

        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList(stateKey))
        }
    }

    override fun onBindViewHolder(holder: PagingViewHolder<T>, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size

    override fun getItemId(position: Int) = list[position].id.toLong()

    fun isEmpty() = list.isEmpty()

    open fun insert(items: Iterable<T>) {
        list.addAll(0, items.filter { !list.contains(it) })

        notifyDataSetChanged()
    }

    fun insert(items: Array<T>) {
        insert(items.asIterable())
    }

    open fun append(items: Iterable<T>) {
        list.addAll(items.filter { !list.contains(it) })

        notifyDataSetChanged()
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
        list.clear()

        notifyDataSetChanged()
    }

    @CallSuper
    open fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(stateKey, list)
    }

    open protected fun contains(item: T): Boolean {
        return list.contains(item)
    }

    abstract class PagingViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

        open fun bind(item: T) {

        }

    }

}