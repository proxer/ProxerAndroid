package me.proxer.app.media.recommendation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.media.recommendation.RecommendationAdapter.ViewHolder
import me.proxer.app.util.extension.colorRes
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.getQuantityString
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entity.info.Recommendation
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class RecommendationAdapter : BaseAdapter<Recommendation, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, Recommendation>> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = data[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_recommendation, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val medium: TextView by bindView(R.id.medium)
        internal val image: ImageView by bindView(R.id.image)
        internal val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        internal val rating: RatingBar by bindView(R.id.rating)
        internal val state: ImageView by bindView(R.id.state)
        internal val episodes: TextView by bindView(R.id.episodes)
        internal val english: ImageView by bindView(R.id.english)
        internal val german: ImageView by bindView(R.id.german)
        internal val upvotesImage: ImageView by bindView(R.id.upvotesImage)
        internal val upvotesText: TextView by bindView(R.id.upvotesText)
        internal val downvotesImage: ImageView by bindView(R.id.downvotesImage)
        internal val downvotesText: TextView by bindView(R.id.downvotesText)

        init {
            upvotesImage.setImageDrawable(generateUpvotesImage())
            downvotesImage.setImageDrawable(generateDownvotesImage())
        }

        fun bind(item: Recommendation) {
            itemView.clicks()
                .mapAdapterPosition({ adapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            ViewCompat.setTransitionName(image, "recommendation_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            episodes.text = episodes.context.getQuantityString(
                when (item.category) {
                    Category.ANIME -> R.plurals.media_episode_count
                    Category.MANGA -> R.plurals.media_chapter_count
                },
                item.episodeAmount
            )

            if (item.rating > 0) {
                ratingContainer.isVisible = true
                rating.rating = item.rating / 2.0f

                episodes.updateLayoutParams<RelativeLayout.LayoutParams> {
                    addRule(RelativeLayout.ALIGN_BOTTOM, 0)
                    addRule(RelativeLayout.BELOW, R.id.state)
                }
            } else {
                ratingContainer.isGone = true

                episodes.updateLayoutParams<RelativeLayout.LayoutParams> {
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.languageContainer)
                    addRule(RelativeLayout.BELOW, R.id.medium)
                }
            }

            when (item.userVote) {
                true -> upvotesImage.setImageDrawable(generateUpvotesImage(true))
                false -> downvotesImage.setImageDrawable(generateUpvotesImage(true))
            }

            upvotesText.text = item.positiveVotes.toString()
            downvotesText.text = item.negativeVotes.toString()

            state.setImageDrawable(item.state.toAppDrawable(state.context))
            glide?.defaultLoad(image, ProxerUrls.entryImage(item.id))
        }

        private fun generateUpvotesImage(userVoted: Boolean = false) = IconicsDrawable(upvotesImage.context)
            .icon(CommunityMaterial.Icon2.cmd_thumb_up)
            .sizeDp(32)
            .paddingDp(4)
            .colorRes(
                upvotesImage.context, when (userVoted) {
                    true -> R.color.md_green_500
                    false -> R.color.icon_unfocused
                }
            )

        private fun generateDownvotesImage(userVoted: Boolean = false) = IconicsDrawable(downvotesImage.context)
            .icon(CommunityMaterial.Icon2.cmd_thumb_down)
            .sizeDp(32)
            .paddingDp(4)
            .colorRes(
                upvotesImage.context, when (userVoted) {
                    true -> R.color.md_red_500
                    false -> R.color.icon_unfocused
                }
            )
    }
}
