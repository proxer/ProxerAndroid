package com.proxerme.app.adapter.media

import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
import com.proxerme.app.util.ParcelableLongSparseArray
import com.proxerme.app.util.TimeUtil
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
class CommentAdapter(savedInstanceState: Bundle? = null) :
        PagingAdapter<Comment, CommentAdapter.CommentAdapterCallback>() {

    private companion object {
        private const val ITEMS_STATE = "adapter_comment_state_items"
        private const val EXPANDED_STATE = "adapter_comment_expanded_items"
        private const val SPOILER_STATE = "adapter_comment_spoiler_items"
        private const val ICON_SIZE = 32
        private const val ICON_PADDING = 8
        private const val ROTATION_HALF = 180f
    }

    private val expandedMap: ParcelableLongSparseArray
    private val spoilerStateMap: HashMap<String, List<Boolean>>

    init {
        if (savedInstanceState == null) {
            expandedMap = ParcelableLongSparseArray()
            spoilerStateMap = HashMap<String, List<Boolean>>()
        } else {
            expandedMap = savedInstanceState.getParcelable(EXPANDED_STATE)
            spoilerStateMap = savedInstanceState.getBundle(SPOILER_STATE).toSpoilerMap()

            list.addAll(savedInstanceState.getParcelableArrayList(ITEMS_STATE))
        }

        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            PagingViewHolder<Comment, CommentAdapterCallback> {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_comment, parent, false))
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(ITEMS_STATE, list)
        outState.putParcelable(EXPANDED_STATE, expandedMap)
        outState.putBundle(SPOILER_STATE, spoilerStateMap.toBundle())
    }

    private fun HashMap<String, List<Boolean>>.toBundle(): Bundle {
        return Bundle().apply {
            entries.forEach {
                this.putIntegerArrayList(it.key, it.value.map {
                    when (it) {
                        true -> 1
                        false -> 0
                    }
                } as ArrayList<Int>)
            }
        }
    }

    private fun Bundle.toSpoilerMap(): HashMap<String, List<Boolean>> {
        return HashMap<String, List<Boolean>>().apply {
            keySet().forEach {
                put(it, getIntegerArrayList(it).map {
                    when (it) {
                        1 -> true
                        0 -> false
                        else -> throw IllegalArgumentException("Illegal mapping for Boolean: $it")
                    }
                })
            }
        }
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
                    val id = adapterList[adapterPosition].id

                    if (expandedMap.get(id.toLong(), false)) {
                        expandedMap.remove(id.toLong())

                        ViewCompat.animate(expand).rotation(0f)
                    } else {
                        expandedMap.put(id.toLong(), true)

                        ViewCompat.animate(expand).rotation(ROTATION_HALF)
                    }

                    notifyItemChanged(adapterPosition)
                }
            }

            userContainer.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onUserClick(list[adapterPosition])
                }
            }
        }

        override fun bind(item: Comment) {
            username.text = item.username

            comment.bbCode = item.comment
            comment.expanded = expandedMap.get(item.id.toLong(), false)
            comment.spoilerStateListener = {
                spoilerStateMap.put(item.id, it)
            }

            comment.setSpoilerStates(spoilerStateMap[item.id])

            if (item.rating <= 0) {
                rating.visibility = View.GONE
            } else {
                rating.visibility = View.VISIBLE
                rating.rating = item.rating.toFloat() / 2.0f
            }

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

            time.text = TimeUtil.convertToRelativeReadableTime(time.context, item.time)
            state.text = convertStateToText(item.state, item.episode)

            if (expandedMap.get(item.id.toLong(), false)) {
                ViewCompat.setRotation(expand, ROTATION_HALF)
            } else {
                ViewCompat.setRotation(expand, 0f)
            }

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

        private fun bindRatingRow(container: ViewGroup, ratingBar: RatingBar, rating: Int) {
            if (rating <= 0) {
                container.visibility = View.GONE
            } else {
                ratingBar.visibility = View.VISIBLE
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

    abstract class CommentAdapterCallback : PagingAdapter.PagingAdapterCallback<Comment>() {
        open fun onUserClick(item: Comment) {

        }
    }

}