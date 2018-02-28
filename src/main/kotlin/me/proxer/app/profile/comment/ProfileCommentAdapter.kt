package me.proxer.app.profile.comment

import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.profile.comment.ProfileCommentAdapter.ViewHolder
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.unsafeLazy

/**
 * @author Ruben Gees
 */
class ProfileCommentAdapter(savedInstanceState: Bundle?) : BaseAdapter<ParsedUserComment, ViewHolder>() {

    private companion object {
        private const val EXPANDED_STATE = "profile_comment_expanded"
    }

    var glide: GlideRequests? = null
    val titleClickSubject: PublishSubject<ParsedUserComment> = PublishSubject.create()

    private var layoutManager: LayoutManager? = null
    private val expansionMap: ParcelableStringBooleanMap

    init {
        expansionMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        layoutManager = recyclerView.layoutManager
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.comment.destroy()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        layoutManager = null
        glide = null
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(EXPANDED_STATE, expansionMap)
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

        private val maxHeight by unsafeLazy { comment.context.resources.displayMetrics.heightPixels / 4 }

        init {
            image.visibility = View.GONE

            expand.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32)

            expand.setOnClickListener {
                withSafeAdapterPosition(this) {
                    expansionMap.putOrRemove(data[it].id)

                    handleExpansion(true)
                }
            }

            titleContainer.setOnClickListener {
                withSafeAdapterPosition(this) {
                    titleClickSubject.onNext(data[it])
                }
            }

            comment.glide = glide
            comment.heightChangedListener = {
                withSafeAdapterPosition(this) {
                    when (comment.maxHeight >= maxHeight) {
                        true -> expansionMap.put(data[it].id, true)
                        false -> expansionMap.remove(data[it].id)
                    }

                    handleExpansion(true)
                }
            }

            upvoteIcon.setIconicsImage(CommunityMaterial.Icon.cmd_thumb_up, 32)
        }

        fun bind(item: ParsedUserComment) {
            ViewCompat.setTransitionName(image, "comment_${item.id}")

            title.text = item.entryName
            upvotes.text = item.helpfulVotes.toString()

            bindRatingRow(ratingGenreRow, ratingGenre, item.ratingDetails.genre.toFloat())
            bindRatingRow(ratingStoryRow, ratingStory, item.ratingDetails.story.toFloat())
            bindRatingRow(ratingAnimationRow, ratingAnimation, item.ratingDetails.animation.toFloat())
            bindRatingRow(ratingCharactersRow, ratingCharacters, item.ratingDetails.characters.toFloat())
            bindRatingRow(ratingMusicRow, ratingMusic, item.ratingDetails.music.toFloat())
            bindRatingRow(ratingOverallRow, ratingOverall, item.overallRating.toFloat() / 2.0f)

            comment.setTree(item.parsedContent)

            time.text = item.date.convertToRelativeReadableTime(time.context)
            progress.text = item.mediaProgress.toEpisodeAppString(progress.context, item.episode, item.category)

            handleExpansion()
        }

        private fun handleExpansion(animate: Boolean = false) {
            withSafeAdapterPosition(this) {
                ViewCompat.animate(expand).cancel()

                if (expansionMap.containsKey(data[it].id)) {
                    comment.maxHeight = Int.MAX_VALUE

                    when (animate) {
                        true -> ViewCompat.animate(expand).rotation(180f)
                        false -> expand.rotation = 180f
                    }
                } else {
                    comment.maxHeight = maxHeight

                    when (animate) {
                        true -> ViewCompat.animate(expand).rotation(0f)
                        false -> expand.rotation = 0f
                    }
                }

                comment.post { bindExpandButton(maxHeight) }

                if (animate) {
                    comment.requestLayout()
                    layoutManager?.requestSimpleAnimationsInNextLayout()
                }
            }
        }

        private fun bindRatingRow(container: ViewGroup, ratingBar: RatingBar, rating: Float) = when (rating <= 0) {
            true -> container.visibility = View.GONE
            false -> {
                container.visibility = View.VISIBLE
                ratingBar.rating = rating
            }
        }

        private fun bindExpandButton(maxHeight: Int) = when (comment.height < maxHeight) {
            true -> expand.visibility = View.GONE
            false -> expand.visibility = View.VISIBLE
        }
    }
}
