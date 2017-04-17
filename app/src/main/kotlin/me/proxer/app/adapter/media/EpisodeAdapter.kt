package me.proxer.app.adapter.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.entity.EpisodeRow
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ParcelableStringBooleanMap
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.find
import org.jetbrains.anko.forEachChildWithIndex

/**
 * @author Ruben Gees
 */
class EpisodeAdapter(savedInstanceState: Bundle?) : PagingAdapter<EpisodeRow>() {

    private companion object {
        private const val EXPANDED_STATE = "episode_expanded"
    }

    private val expanded: ParcelableStringBooleanMap

    var callback: EpisodeAdapterCallback? = null

    init {
        expanded = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return list[position].number.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<EpisodeRow> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false))
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(EXPANDED_STATE, expanded)
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<EpisodeRow>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val titleContainer: ViewGroup by bindView(R.id.titleContainer)
        private val watched: ImageView by bindView(R.id.watched)
        private val languages: ViewGroup by bindView(R.id.languages)

        init {
            titleContainer.setOnClickListener {
                withSafeAdapterPosition {
                    val number = list[it].number.toString()

                    if (expanded[number] ?: false) {
                        expanded.remove(number)
                    } else {
                        expanded.put(number, true)
                    }

                    notifyItemChanged(it)
                }
            }

            watched.setImageDrawable(IconicsDrawable(watched.context)
                    .icon(CommunityMaterial.Icon.cmd_check)
                    .sizeDp(24)
                    .colorRes(R.color.icon))
        }

        override fun bind(item: EpisodeRow) {
            title.text = item.title ?: item.category.toEpisodeAppString(title.context, item.number)

            if (item.userProgress >= item.number) {
                watched.visibility = View.VISIBLE
            } else {
                watched.visibility = View.INVISIBLE
            }

            if (expanded[item.number.toString()] ?: false) {
                languages.visibility = View.VISIBLE
            } else {
                languages.visibility = View.GONE

                return
            }

            if (languages.childCount != item.languageHosterList.size) {
                languages.removeAllViews()

                for (i in 0 until item.languageHosterList.size) {
                    View.inflate(languages.context, R.layout.layout_episode_language, languages)
                }
            }

            item.languageHosterList.forEachWithIndex { index, (language, hosterImages) ->
                val languageContainer = languages.getChildAt(index)
                val languageView = languageContainer.find<TextView>(R.id.language)
                val hostersView = languageContainer.find<ViewGroup>(R.id.hosters)

                languageView.text = ProxerUtils.getApiEnumName(language)
                languageView.setCompoundDrawablesWithIntrinsicBounds(language.toGeneralLanguage()
                        .toAppDrawable(languageView.context), null, null, null)

                languageContainer.setOnClickListener {
                    withSafeAdapterPosition {
                        callback?.onLanguageClick(language, list[it])
                    }
                }

                if (hosterImages == null || hosterImages.isEmpty()) {
                    hostersView.removeAllViews()
                    hostersView.visibility = View.GONE
                } else {
                    hostersView.visibility = View.VISIBLE

                    if (hostersView.childCount != hosterImages.size) {
                        hostersView.removeAllViews()

                        for (i in 0 until hosterImages.size) {
                            val imageView = LayoutInflater.from(hostersView.context)
                                    .inflate(R.layout.layout_image, hostersView, false).apply {
                                layoutParams.width = DeviceUtils.convertDpToPx(hostersView.context, 28f)
                                layoutParams.height = DeviceUtils.convertDpToPx(hostersView.context, 28f)
                            }

                            hostersView.addView(imageView)
                        }
                    }

                    hostersView.forEachChildWithIndex { index, imageView ->
                        Glide.with(imageView.context)
                                .load(ProxerUrls.hosterImage(hosterImages[index]).toString())
                                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                .into(imageView as ImageView)
                    }
                }
            }
        }
    }

    abstract class EpisodeAdapterCallback {
        open fun onLanguageClick(language: MediaLanguage, episode: EpisodeRow) {

        }
    }
}