package com.proxerme.app.adapter.media

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.entitiy.RichEpisode
import com.proxerme.app.util.ParcelableBooleanSparseArray
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.parameters.LanguageParameter.Language
import org.jetbrains.anko.find
import org.jetbrains.anko.forEachChildWithIndex

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class EpisodeAdapter(savedInstanceState: Bundle? = null) :
        PagingAdapter<RichEpisode, EpisodeAdapter.EpisodeAdapterCallback>() {

    private companion object {
        private const val ITEMS_STATE = "adapter_episode_state_items"
        private const val EXPANDED_STATE = "adapter_episode_expanded_items"
    }

    private val expanded: ParcelableBooleanSparseArray

    init {
        if (savedInstanceState == null) {
            expanded = ParcelableBooleanSparseArray()
        } else {
            expanded = savedInstanceState.getParcelable(EXPANDED_STATE)

            list.addAll(savedInstanceState.getParcelableArrayList(ITEMS_STATE))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            PagingViewHolder<RichEpisode, EpisodeAdapterCallback> {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_episode, parent, false))
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(ITEMS_STATE, list)
        outState.putParcelable(EXPANDED_STATE, expanded)
    }

    open class EpisodeAdapterCallback : PagingAdapter.PagingAdapterCallback<RichEpisode>() {
        open fun onLanguageClick(@Language language: String, episode: RichEpisode) {

        }
    }

    inner class ViewHolder(itemView: View) :
            PagingViewHolder<RichEpisode, EpisodeAdapterCallback>(itemView) {

        override val adapterList: List<RichEpisode>
            get() = list
        override val adapterCallback: EpisodeAdapterCallback?
            get() = callback
        override val allowOnRootClick = false

        private val title: TextView by bindView(R.id.title)
        private val titleContainer: ViewGroup by bindView(R.id.titleContainer)
        private val watched: ImageView by bindView(R.id.watched)
        private val languages: ViewGroup by bindView(R.id.languages)

        init {
            titleContainer.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val number = list[adapterPosition].number

                    if (expanded.get(number)) {
                        expanded.delete(number)
                    } else {
                        expanded.put(number, true)
                    }

                    notifyItemChanged(adapterPosition)
                }

                languages.visibility = View.VISIBLE
            }

            watched.setImageDrawable(IconicsDrawable(watched.context)
                    .icon(CommunityMaterial.Icon.cmd_check)
                    .sizeDp(24)
                    .colorRes(R.color.icon))
        }

        override fun bind(item: RichEpisode) {
            if (item.title == null) {
                title.text = "Episode ${item.number}"
            } else {
                title.text = item.title
            }

            if (item.userState >= item.number) {
                watched.visibility = View.VISIBLE
            } else {
                watched.visibility = View.INVISIBLE
            }

            if (languages.childCount < item.languageHosterMap.size) {
                for (i in 0 until item.languageHosterMap.size - languages.childCount) {
                    View.inflate(languages.context, R.layout.layout_episode_language, languages)
                }
            } else if (languages.childCount > item.languageHosterMap.size) {
                for (i in 0 until languages.childCount - item.languageHosterMap.size) {
                    languages.removeViewAt(0)
                }
            }

            populateLanguages(item)

            if (expanded.get(item.number)) {
                languages.visibility = View.VISIBLE
            } else {
                languages.visibility = View.GONE
            }
        }

        private fun populateLanguages(item: RichEpisode) {
            var index = 0

            item.languageHosterMap.forEach { languageEntry ->
                val languageContainer = languages.getChildAt(index)
                val language = languageContainer.find<TextView>(R.id.language)
                val hosters = languageContainer.find<ViewGroup>(R.id.hosters)

                language.text = languageEntry.key
                languageContainer.setOnClickListener {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        callback?.onLanguageClick(languageEntry.key, item)
                    }
                }

                if (languageEntry.value == null) {
                    hosters.removeAllViews()
                    hosters.visibility = View.GONE
                } else {
                    hosters.visibility = View.VISIBLE

                    if (hosters.childCount < languageEntry.value!!.size) {
                        for (i in 0 until languageEntry.value!!.size - hosters.childCount) {
                            View.inflate(hosters.context, R.layout.item_hoster, hosters)
                        }
                    } else if (hosters.childCount > languageEntry.value!!.size) {
                        for (i in 0 until hosters.childCount - languageEntry.value!!.size) {
                            hosters.removeViewAt(0)
                        }
                    }

                    hosters.forEachChildWithIndex { position, view ->
                        view as ImageView

                        Glide.with(view.context)
                                .load(ProxerUrlHolder
                                        .getHosterImageUrl(languageEntry.value!![position].imageId)
                                        .toString())
                                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                .into(view)
                    }
                }

                index++
            }
        }
    }
}