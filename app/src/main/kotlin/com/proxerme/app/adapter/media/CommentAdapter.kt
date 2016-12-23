package com.proxerme.app.adapter.media

import android.support.v4.util.LongSparseArray
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.DeviceUtils
import com.proxerme.app.util.TimeUtils
import com.proxerme.app.util.bindView
import com.proxerme.app.view.bbcode.BBCodeView
import com.proxerme.library.connection.info.entity.Comment
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.CommentStateParameter.*
import java.util.*

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class CommentAdapter : PagingAdapter<Comment>() {

    private companion object {
        private const val ICON_SIZE = 32
        private const val ICON_PADDING = 8
        private const val ROTATION_HALF = 180f
    }

    private val expandedMap = LongSparseArray<Boolean>()
    private val spoilerStateMap = HashMap<String, List<Boolean>>()

    var callback: CommentAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<Comment> {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_comment, parent, false))
    }

    override fun removeCallback() {
        callback = null
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<Comment>(itemView) {

        private val userContainer: ViewGroup by bindView(R.id.userContainer)
        private val userImage: ImageView by bindView(R.id.userImage)
        private val username: TextView by bindView(R.id.username)

        private val upvoteIcon: ImageView by bindView(R.id.upvoteIcon)
        private val upvotes: TextView by bindView(R.id.upvotes)

        private val ratingOverallRow: ViewGroup by bindView(R.id.ratingOverallRow)
        private val ratingOverall: RatingBar by bindView(R.id.ratingOverall)
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
        private val expand: ImageButton by bindView(R.id.expand)

        private val time: TextView by bindView(R.id.time)
        private val state: TextView by bindView(R.id.state)

        init {
            expand.setImageDrawable(IconicsDrawable(expand.context)
                    .colorRes(R.color.icon)
                    .sizeDp(ICON_SIZE)
                    .paddingDp(ICON_PADDING)
                    .icon(CommunityMaterial.Icon.cmd_chevron_down))

            expand.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val id = list[adapterPosition].id

                    if (expandedMap.get(id.toLong(), false)) {
                        expandedMap.remove(id.toLong())
                    } else {
                        expandedMap.put(id.toLong(), true)
                    }

                    notifyItemChanged(adapterPosition)
                }
            }

            userContainer.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onUserClick(list[adapterPosition])
                }
            }

            upvoteIcon.setImageDrawable(IconicsDrawable(expand.context)
                    .colorRes(R.color.icon)
                    .sizeDp(ICON_SIZE)
                    .paddingDp(ICON_PADDING)
                    .icon(CommunityMaterial.Icon.cmd_thumb_up))
        }

        override fun bind(item: Comment) {
            username.text = item.username
            upvotes.text = item.helpfulVotes.toString()

            bindComment(item)
            bindExpanded(item)

            bindRatingRow(ratingGenreRow, ratingGenre,
                    item.ratingDetails.genre)
            bindRatingRow(ratingStoryRow, ratingStory,
                    item.ratingDetails.story)
            bindRatingRow(ratingAnimationRow, ratingAnimation,
                    item.ratingDetails.animation)
            bindRatingRow(ratingCharactersRow, ratingCharacters,
                    item.ratingDetails.characters)
            bindRatingRow(ratingMusicRow, ratingMusic,
                    item.ratingDetails.music)
            bindRatingRow(ratingOverallRow, ratingOverall, (item.rating.toFloat() / 2.0f).toInt())

            time.text = TimeUtils.convertToRelativeReadableTime(time.context, item.time)
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

        private fun bindExpanded(item: Comment) {
            if (expandedMap.get(item.id.toLong(), false)) {
                comment.maxHeight = Int.MAX_VALUE

                ViewCompat.animate(expand).rotation(ROTATION_HALF)
            } else {
                comment.maxHeight = DeviceUtils.convertDpToPx(comment.context, 150f)

                ViewCompat.animate(expand).rotation(0f)
            }
        }

        private fun bindComment(item: Comment) {
            comment.bbCode = item.comment
            comment.expanded = expandedMap.get(item.id.toLong(), false)
            comment.spoilerStateListener = {
                spoilerStateMap.put(item.id, it)
            }

            comment.setSpoilerStates(spoilerStateMap[item.id])
        }

        private fun bindRatingRow(container: ViewGroup, ratingBar: RatingBar, rating: Int) {
            if (rating <= 0) {
                container.visibility = View.GONE
            } else {
                container.visibility = View.VISIBLE
                ratingBar.rating = rating.toFloat()
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

    abstract class CommentAdapterCallback {
        open fun onUserClick(item: Comment) {

        }
    }

}