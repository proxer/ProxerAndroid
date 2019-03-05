package me.proxer.app.media.episode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.forEachIndexed
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.typeface.library.communitymaterial.CommunityMaterial
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.media.episode.EpisodeAdapter.ViewHolder
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.getSafeParcelable
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.recursiveChildren
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class EpisodeAdapter(savedInstanceState: Bundle?) : BaseAdapter<EpisodeRow, ViewHolder>() {

    private companion object {
        private const val EXPANDED_STATE = "episode_expanded"
    }

    var glide: GlideRequests? = null
    val languageClickSubject: PublishSubject<Pair<MediaLanguage, EpisodeRow>> = PublishSubject.create()

    private val expansionMap: ParcelableStringBooleanMap

    init {
        expansionMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getSafeParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])
    override fun getItemId(position: Int) = data[position].number.toLong()

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.languages.recursiveChildren
            .filterIsInstance(ImageView::class.java)
            .forEach { glide?.clear(it) }
    }

    override fun areItemsTheSame(old: EpisodeRow, new: EpisodeRow) = old.number == new.number
    override fun saveInstanceState(outState: Bundle) = outState.putParcelable(EXPANDED_STATE, expansionMap)

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val titleContainer: ViewGroup by bindView(R.id.titleContainer)
        internal val watched: ImageView by bindView(R.id.watched)
        internal val languages: ViewGroup by bindView(R.id.languages)

        init {
            watched.setIconicsImage(CommunityMaterial.Icon.cmd_check, 24, 0)
        }

        fun bind(item: EpisodeRow) {
            titleContainer.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[it].number.toString() to it }
                .autoDisposable(this)
                .subscribe { (number, position) ->
                    expansionMap.putOrRemove(number)

                    notifyItemChanged(position)
                }

            title.text = item.title ?: item.category.toEpisodeAppString(title.context, item.number)

            if (item.userProgress ?: 0 >= item.number) {
                watched.isVisible = true
            } else {
                watched.isInvisible = true
            }

            if (expansionMap.containsKey(item.number.toString())) {
                languages.isVisible = true
            } else {
                languages.isGone = true

                return
            }

            if (languages.childCount != item.languageHosterList.size) {
                languages.removeAllViews()

                repeat(item.languageHosterList.size) {
                    View.inflate(languages.context, R.layout.layout_episode_language, languages)
                }
            }

            item.languageHosterList.withIndex().forEach { (index, languageAndHosterImages) ->
                val (language, hosterImages) = languageAndHosterImages

                val languageContainer = languages.getChildAt(index)
                val languageView = languageContainer.findViewById<TextView>(R.id.language)
                val hostersView = languageContainer.findViewById<ViewGroup>(R.id.hosters)

                languageView.text = language.toAppString(languageView.context)
                languageView.setCompoundDrawablesWithIntrinsicBounds(
                    language.toGeneralLanguage().toAppDrawable(languageView.context), null, null, null
                )

                languageContainer.clicks()
                    .mapAdapterPosition({ adapterPosition }) { language to data[it] }
                    .autoDisposable(this)
                    .subscribe(languageClickSubject)

                bindHosterImages(hosterImages, hostersView)
            }
        }

        private fun bindHosterImages(hosterImages: List<String>?, hostersView: ViewGroup) {
            if (hosterImages == null || hosterImages.isEmpty()) {
                hostersView.removeAllViews()

                hostersView.isGone = true
            } else {
                hostersView.isVisible = true

                if (hostersView.childCount != hosterImages.size) {
                    hostersView.removeAllViews()

                    repeat(hosterImages.size) {
                        val inflater = LayoutInflater.from(hostersView.context)
                        val imageView = inflater.inflate(R.layout.layout_image, hostersView, false).apply {
                            layoutParams.width = dip(28)
                            layoutParams.height = dip(28)
                        }

                        hostersView.addView(imageView)
                    }
                }

                hostersView.forEachIndexed { index, imageView ->
                    glide?.defaultLoad(imageView as ImageView, ProxerUrls.hosterImage(hosterImages[index]))
                }
            }
        }
    }
}
