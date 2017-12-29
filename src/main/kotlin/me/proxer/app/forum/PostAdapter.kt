package me.proxer.app.forum

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.forum.PostAdapter.ViewHolder
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.library.entity.forum.Post

/**
 * @author Ruben Gees
 */
class PostAdapter : BaseAdapter<Post, ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_topic, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    inner class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {

        internal val post by bindView<BBCodeView>(R.id.post)

        fun bind(item: Post) {
            post.text = item.message
        }
    }
}
