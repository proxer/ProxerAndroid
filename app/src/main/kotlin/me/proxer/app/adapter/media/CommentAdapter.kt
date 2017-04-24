package me.proxer.app.adapter.media

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.view.ViewCompat
import android.util.SparseBooleanArray
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
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.util.ParcelableStringBooleanMap
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.view.bbcode.BBCodeView
import me.proxer.library.entitiy.info.Comment
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class CommentAdapter(savedInstanceState: Bundle?) : PagingAdapter<Comment>() {

    private companion object {
        private const val EXPANDED_STATE = "comments_expanded"
        private const val SPOILER_STATES_STATE = "comments_spoiler_states"
    }

    private val expanded: ParcelableStringBooleanMap
    private val spoilerStates: ParcelableSpoilerStateMap

    var callback: CommentAdapterCallback? = null

    init {
        expanded = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        spoilerStates = when (savedInstanceState) {
            null -> ParcelableSpoilerStateMap()
            else -> savedInstanceState.getParcelable(SPOILER_STATES_STATE)
        }

        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = internalList[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<Comment> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false))
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(EXPANDED_STATE, expanded)
        outState.putParcelable(SPOILER_STATES_STATE, spoilerStates)
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
        private val progress: TextView by bindView(R.id.progress)

        init {
            expand.setImageDrawable(IconicsDrawable(expand.context)
                    .colorRes(R.color.icon)
                    .sizeDp(32)
                    .paddingDp(8)
                    .icon(CommunityMaterial.Icon.cmd_chevron_down))

            expand.setOnClickListener {
                withSafeAdapterPosition {
                    val id = internalList[it].id

                    if (expanded[id] ?: false) {
                        expanded.remove(id)
                    } else {
                        expanded.put(id, true)
                    }

                    notifyItemChanged(it)
                }
            }

            userContainer.setOnClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onUserClick(view, internalList[it])
                }
            }

            upvoteIcon.setImageDrawable(IconicsDrawable(expand.context)
                    .colorRes(R.color.icon)
                    .sizeDp(32)
                    .paddingDp(8)
                    .icon(CommunityMaterial.Icon.cmd_thumb_up))
        }

        override fun bind(item: Comment) {
            val maxHeight = comment.context.resources.displayMetrics.heightPixels / 4

            ViewCompat.setTransitionName(userImage, "comment_${item.id}")

            username.text = item.author
            upvotes.text = item.helpfulVotes.toString()

            bindRatingRow(ratingGenreRow, ratingGenre, item.ratingDetails.genre.toFloat())
            bindRatingRow(ratingStoryRow, ratingStory, item.ratingDetails.story.toFloat())
            bindRatingRow(ratingAnimationRow, ratingAnimation, item.ratingDetails.animation.toFloat())
            bindRatingRow(ratingCharactersRow, ratingCharacters, item.ratingDetails.characters.toFloat())
            bindRatingRow(ratingMusicRow, ratingMusic, item.ratingDetails.music.toFloat())
            bindRatingRow(ratingOverallRow, ratingOverall, item.overallRating.toFloat() / 2.0f)

            comment.text = item.content
            time.text = TimeUtils.convertToRelativeReadableTime(time.context, item.date)
            progress.text = item.mediaProgress.toEpisodeAppString(progress.context, item.episode)

            comment.spoilerStates = spoilerStates[item.id] ?: SparseBooleanArray(0)
            comment.spoilerStateListener = { states, hasBeenExpanded ->
                spoilerStates.put(item.id, states)

                if (hasBeenExpanded) {
                    if (!(expanded[item.id] ?: false)) {
                        expanded.put(item.id, true)

                        comment.maxHeight = Int.MAX_VALUE
                        comment.post {
                            bindExpandButton(maxHeight)
                        }

                        ViewCompat.animate(expand).rotation(180f)
                    }
                } else {
                    comment.post {
                        bindExpandButton(maxHeight)
                    }
                }
            }

            if (expanded[item.id] ?: false) {
                comment.maxHeight = Int.MAX_VALUE

                ViewCompat.animate(expand).rotation(180f)
            } else {
                comment.maxHeight = maxHeight

                ViewCompat.animate(expand).rotation(0f)
            }

            comment.post {
                bindExpandButton(maxHeight)
            }

            bindImage(item)
        }

        private fun bindRatingRow(container: ViewGroup, ratingBar: RatingBar, rating: Float) {
            if (rating <= 0) {
                container.visibility = View.GONE
            } else {
                container.visibility = View.VISIBLE
                ratingBar.rating = rating
            }
        }

        private fun bindExpandButton(maxHeight: Int) {
            if (comment.height < maxHeight) {
                expand.visibility = View.GONE
            } else {
                expand.visibility = View.VISIBLE
            }
        }

        private fun bindImage(item: Comment) {
            if (item.image.isBlank()) {
                Glide.clear(userImage)

                userImage.setImageDrawable(IconicsDrawable(userImage.context)
                        .icon(CommunityMaterial.Icon.cmd_account)
                        .sizeDp(96)
                        .paddingDp(16)
                        .colorRes(R.color.colorAccent))
            } else {
                Glide.with(userImage.context)
                        .load(ProxerUrls.userImage(item.image).toString())
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(userImage)
            }
        }
    }

    interface CommentAdapterCallback {
        fun onUserClick(view: View, item: Comment) {}
    }

    internal class ParcelableSpoilerStateMap : Parcelable {

        companion object {

            @Suppress("unused")
            @JvmStatic val CREATOR = object : Parcelable.Creator<ParcelableSpoilerStateMap> {
                override fun createFromParcel(source: Parcel): ParcelableSpoilerStateMap {
                    return ParcelableSpoilerStateMap(source)
                }

                override fun newArray(size: Int): Array<ParcelableSpoilerStateMap?> {
                    return arrayOfNulls(size)
                }
            }
        }

        private val internalMap = LinkedHashMap<String, SparseBooleanArray>()

        constructor() : super()
        internal constructor(source: Parcel) {
            (0 until source.readInt()).forEach {
                internalMap.put(source.readString(), source.readSparseBooleanArray())
            }
        }

        override fun describeContents() = 0
        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(internalMap.size)

            internalMap.entries.forEach {
                dest.writeString(it.key)
                dest.writeSparseBooleanArray(it.value)
            }
        }

        fun put(key: String, value: SparseBooleanArray) = internalMap.put(key, value)
        operator fun get(key: String) = internalMap[key]
    }
}