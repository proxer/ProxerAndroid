package com.proxerme.app.adapter.media

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import butterknife.bindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.TimeUtil
import com.proxerme.app.view.BBCodeView
import com.proxerme.library.connection.info.entity.Comment
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CommentStateParameter.*

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

        private val rating: RatingBar by bindView(R.id.rating)
        private val ratingGenre: RatingBar by bindView(R.id.ratingGenre)
        private val ratingGenreRow: ViewGroup by bindView(R.id.ratingGenreRow)
        private val ratingStory: RatingBar by bindView(R.id.ratingStory)
        private val ratingStoryRow: ViewGroup by bindView(R.id.ratingStoryRow)
        private val ratingAnimation: RatingBar by bindView(R.id.ratingAnimation)
        private val ratingAnimationRow: ViewGroup by bindView(R.id.ratingAnimationRow)
        private val ratingCharacters: RatingBar by bindView(R.id.ratingCharacters)
        private val ratingCharactersRow: ViewGroup by bindView(R.id.ratingCharactersRow)
        private val ratingMusic: RatingBar by bindView(R.id.ratingMusic)
        private val ratingMusicRow: ViewGroup by bindView(R.id.ratingMusicRow)

        private val comment: BBCodeView by bindView(R.id.comment)

        private val time: TextView by bindView(R.id.time)
        private val state: TextView by bindView(R.id.state)

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
            rating.rating = item.rating.toFloat() / 2.0f

            bindRatingRow(ratingGenreRow, ratingGenre,
                    item.ratingDetails.genre.toFloat())
            bindRatingRow(ratingStoryRow, ratingStory,
                    item.ratingDetails.story.toFloat())
            bindRatingRow(ratingAnimationRow, ratingAnimation,
                    item.ratingDetails.animation.toFloat())
            bindRatingRow(ratingCharactersRow, ratingCharacters,
                    item.ratingDetails.characters.toFloat())
            bindRatingRow(ratingMusicRow, ratingMusic,
                    item.ratingDetails.music.toFloat())

            time.text = TimeUtil.convertToRelativeReadableTime(time.context, item.time)
            state.text = convertStateToText(item.state, item.episode)

            if (item.imageId.isBlank()) {
                userImage.setImageDrawable(IconicsDrawable(userImage.context)
                        .icon(CommunityMaterial.Icon.cmd_account)
                        .sizeDp(96)
                        .paddingDp(16)
                        .colorRes(R.color.colorAccent))
            } else {
                Glide.with(userImage.context)
                        .load(ProxerUrlHolder.getUserImageUrl(item.imageId).toString())
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(userImage)
            }
        }

        private fun bindRatingRow(container: ViewGroup, ratingBar: RatingBar, rating: Float) {
            if (rating <= 0) {
                container.visibility = View.GONE
            } else {
                ratingBar.visibility = View.VISIBLE
                ratingBar.rating = rating
            }
        }

        fun convertStateToText(@CommentState state: Int, episode: Int): String {
            return when (state) {
                WATCHED -> itemView.context.getString(R.string.comment_state_watched)
                WATCHING -> itemView.context.getString(R.string.comment_state_watching, episode)
                WILL_WATCH -> itemView.context.getString(R.string.comment_state_will_watch)
                CANCELLED -> itemView.context.getString(R.string.comment_state_cancelled, episode)
                else -> throw IllegalArgumentException("Illegal comment state: $state")
            }
        }
    }

    abstract class CommentAdapterCallback : PagingAdapter.PagingAdapterCallback<Comment>() {
        open fun onUserClick(item: Comment) {

        }
    }

}