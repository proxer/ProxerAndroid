package com.proxerme.app.adapter

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.library.connection.user.entitiy.ToptenEntry
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CategoryParameter
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ToptenAdapter(savedInstanceState: Bundle?,
                    @CategoryParameter.Category private val category: String) :
        RecyclerView.Adapter<ToptenAdapter.ViewHolder>() {

    private companion object {
        const val STATE_ITEMS = "adapter_topten_state_items"
    }

    private val list = ArrayList<ToptenEntry>()

    init {
        setHasStableIds(true)

        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList("${STATE_ITEMS}_$category"))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_topten_entry, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(list[position])

    override fun getItemCount() = list.size

    override fun getItemId(position: Int): Long {
        return list[position].id.toLong()
    }

    fun setItems(newItems: Collection<ToptenEntry>) {
        list.clear()
        list.addAll(newItems)

        notifyDataSetChanged()
    }

    fun clear() {
        list.clear()

        notifyDataSetChanged()
    }

    fun isEmpty() = list.isEmpty()

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList("${STATE_ITEMS}_$category", list)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val image: ImageView by bindView(R.id.image)
        private val title: TextView by bindView(R.id.title)

        init {
            itemView.setOnClickListener {
                //TODO
            }
        }

        fun bind(entry: ToptenEntry) {
            title.text = entry.name

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(entry.id).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }

    }

}