package com.proxerme.app.adapter.media

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
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.view.BBCodeView
import com.proxerme.library.connection.info.entity.Comment
import com.proxerme.library.info.ProxerUrlHolder

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CommentAdapter(savedInstanceState: Bundle? = null) :
        PagingAdapter<Comment, CommentAdapter.CommentAdapterCallback>() {

    private companion object {
        private const val ITEMS_STATE = "adapter_comment_state_items"
    }

    init {
        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList(ITEMS_STATE))
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            PagingViewHolder<Comment, CommentAdapterCallback> {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_comment, parent, false))
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(ITEMS_STATE, list)
    }

    inner class ViewHolder(itemView: View) :
            PagingViewHolder<Comment, CommentAdapterCallback>(itemView) {

        override val adapterList: List<Comment>
            get() = list
        override val adapterCallback: CommentAdapterCallback?
            get() = callback

        private val userContainer: ViewGroup by bindView(R.id.userContainer)
        private val userImage: ImageView by bindView(R.id.userImage)
        private val username: TextView by bindView(R.id.username)
        private val comment: BBCodeView by bindView(R.id.comment)

        init {
            userContainer.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onUserClick(list[adapterPosition])
                }
            }
        }

        override fun bind(item: Comment) {
            username.text = item.username
            comment.bbCode = item.comment

            Glide.with(userImage.context)
                    .load(ProxerUrlHolder.getUserImageUrl(item.imageId).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(userImage)
        }
    }

    abstract class CommentAdapterCallback : PagingAdapter.PagingAdapterCallback<Comment>() {
        open fun onUserClick(item: Comment) {

        }
    }

}