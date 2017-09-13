package me.proxer.app.media.recommendation

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.media.recommendation.RecommendationAdapter.ViewHolder
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.getQuantityString
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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        glide = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

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
            upvotesImage.setImageDrawable(IconicsDrawable(upvotesImage.context, CommunityMaterial.Icon.cmd_thumb_up)
                    .colorRes(R.color.icon)
                    .sizeDp(32)
                    .paddingDp(4))

            downvotesImage.setImageDrawable(IconicsDrawable(upvotesImage.context, CommunityMaterial.Icon.cmd_thumb_down)
                    .colorRes(R.color.icon)
                    .sizeDp(32)
                    .paddingDp(4))

            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(image to data[it])
                }
            }
        }

        fun bind(item: Recommendation) {
            ViewCompat.setTransitionName(image, "recommendation_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            episodes.text = episodes.context.getQuantityString(when (item.category) {
                Category.ANIME -> R.plurals.media_episode_count
                Category.MANGA -> R.plurals.media_chapter_count
            }, item.episodeAmount)

            if (item.rating > 0) {
                ratingContainer.visibility = View.VISIBLE
                rating.rating = item.rating / 2.0f

                (episodes.layoutParams as RelativeLayout.LayoutParams).apply {
                    addRule(RelativeLayout.ALIGN_BOTTOM, 0)
                    addRule(RelativeLayout.BELOW, R.id.state)
                }
            } else {
                ratingContainer.visibility = View.GONE

                (episodes.layoutParams as RelativeLayout.LayoutParams).apply {
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.languageContainer)
                    addRule(RelativeLayout.BELOW, R.id.medium)
                }
            }

            upvotesText.text = item.positiveVotes.toString()
            downvotesText.text = item.negativeVotes.toString()

            state.setImageDrawable(item.state.toAppDrawable(state.context))
            glide?.defaultLoad(image, ProxerUrls.entryImage(item.id))
        }
    }
}
