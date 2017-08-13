package me.proxer.app.profile.comment

import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.profile.comment.ProfileCommentAdapter.ViewHolder
import me.proxer.app.util.data.ParcelableStringBooleanArrayMap
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.view.bbcode.BBCodeView
import me.proxer.library.entitiy.user.UserComment

/**
 * @author Ruben Gees
 */
class ProfileCommentAdapter(savedInstanceState: Bundle?) : BaseAdapter<UserComment, ViewHolder>() {

    private companion object {
        private const val EXPANDED_STATE = "profile_comment_expanded"
        private const val SPOILER_STATES_STATE = "profile_comment_spoiler"
    }

    val titleClickSubject: PublishSubject<UserComment> = PublishSubject.create()

    private val expanded: ParcelableStringBooleanMap
    private val spoilerStates: ParcelableStringBooleanArrayMap

    init {
        expanded = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        spoilerStates = when (savedInstanceState) {
            null -> ParcelableStringBooleanArrayMap()
            else -> savedInstanceState.getParcelable(SPOILER_STATES_STATE)
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(EXPANDED_STATE, expanded)
        outState.putParcelable(SPOILER_STATES_STATE, spoilerStates)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val titleContainer: ViewGroup by bindView(R.id.titleContainer)
        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)

        internal val upvoteIcon: ImageView by bindView(R.id.upvoteIcon)
        internal val upvotes: TextView by bindView(R.id.upvotes)

        internal val ratingOverallRow: ViewGroup by bindView(R.id.ratingOverallRow)
        internal val ratingOverall: RatingBar by bindView(R.id.ratingOverall)
        internal val ratingGenre: RatingBar by bindView(R.id.ratingGenre)
        internal val ratingGenreRow: ViewGroup by bindView(R.id.ratingGenreRow)
        internal val ratingStory: RatingBar by bindView(R.id.ratingStory)
        internal val ratingStoryRow: ViewGroup by bindView(R.id.ratingStoryRow)
        internal val ratingAnimation: RatingBar by bindView(R.id.ratingAnimation)
        internal val ratingAnimationRow: ViewGroup by bindView(R.id.ratingAnimationRow)
        internal val ratingCharacters: RatingBar by bindView(R.id.ratingCharacters)
        internal val ratingCharactersRow: ViewGroup by bindView(R.id.ratingCharactersRow)
        internal val ratingMusic: RatingBar by bindView(R.id.ratingMusic)
        internal val ratingMusicRow: ViewGroup by bindView(R.id.ratingMusicRow)

        internal val comment: BBCodeView by bindView(R.id.comment)
        internal val expand: ImageButton by bindView(R.id.expand)

        internal val time: TextView by bindView(R.id.time)
        internal val progress: TextView by bindView(R.id.progress)

        init {
            image.visibility = View.GONE

            expand.setImageDrawable(IconicsDrawable(expand.context)
                    .colorRes(R.color.icon)
                    .sizeDp(32)
                    .paddingDp(8)
                    .icon(CommunityMaterial.Icon.cmd_chevron_down))

            expand.setOnClickListener {
                withSafeAdapterPosition(this) {
                    data[it].id.let { id ->
                        if (expanded[id] == true) {
                            expanded.remove(id)
                        } else {
                            expanded.put(id, true)
                        }
                    }

                    notifyItemChanged(it)
                }
            }

            titleContainer.setOnClickListener {
                withSafeAdapterPosition(this) {
                    titleClickSubject.onNext(data[it])
                }
            }

            upvoteIcon.setImageDrawable(IconicsDrawable(expand.context)
                    .colorRes(R.color.icon)
                    .sizeDp(32)
                    .paddingDp(8)
                    .icon(CommunityMaterial.Icon.cmd_thumb_up))
        }

        fun bind(item: UserComment) {
            val maxHeight = comment.context.resources.displayMetrics.heightPixels / 4

            ViewCompat.setTransitionName(image, "comment_${item.id}")

            title.text = item.entryName
            upvotes.text = item.helpfulVotes.toString()

            bindRatingRow(ratingGenreRow, ratingGenre, item.ratingDetails.genre.toFloat())
            bindRatingRow(ratingStoryRow, ratingStory, item.ratingDetails.story.toFloat())
            bindRatingRow(ratingAnimationRow, ratingAnimation, item.ratingDetails.animation.toFloat())
            bindRatingRow(ratingCharactersRow, ratingCharacters, item.ratingDetails.characters.toFloat())
            bindRatingRow(ratingMusicRow, ratingMusic, item.ratingDetails.music.toFloat())
            bindRatingRow(ratingOverallRow, ratingOverall, item.overallRating.toFloat() / 2.0f)

            comment.text = item.content
            time.text = item.date.convertToRelativeReadableTime(time.context)
            progress.text = item.mediaProgress.toEpisodeAppString(progress.context, item.episode, item.category)

            comment.spoilerStates = spoilerStates[item.id] ?: SparseBooleanArray(0)
            comment.spoilerStateListener = { states, isExpanded ->
                spoilerStates.put(item.id, states)

                if (isExpanded) {
                    if (expanded[item.id] != true) {
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

            if (expanded[item.id] == true) {
                comment.maxHeight = Int.MAX_VALUE

                ViewCompat.animate(expand).rotation(180f)
            } else {
                comment.maxHeight = maxHeight

                ViewCompat.animate(expand).rotation(0f)
            }

            comment.post {
                bindExpandButton(maxHeight)
            }
        }

        private fun bindRatingRow(container: ViewGroup, ratingBar: RatingBar, rating: Float) = if (rating <= 0) {
            container.visibility = View.GONE
        } else {
            container.visibility = View.VISIBLE
            ratingBar.rating = rating
        }

        private fun bindExpandButton(maxHeight: Int) = if (comment.height < maxHeight) {
            expand.visibility = View.GONE
        } else {
            expand.visibility = View.VISIBLE
        }
    }
}
