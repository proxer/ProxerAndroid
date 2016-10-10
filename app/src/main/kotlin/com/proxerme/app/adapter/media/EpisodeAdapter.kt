package com.proxerme.app.adapter.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.entitiy.RichEpisode
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
        private const val REAL_ITEM_COUNT_STATE = "adapter_episode_state_real_item_count"
    }

    init {
        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList(ITEMS_STATE))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            PagingViewHolder<RichEpisode, EpisodeAdapterCallback> {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_episode, parent, false))
    }

    override fun clear() {
        super.clear()
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(ITEMS_STATE, list)
    }

    open class EpisodeAdapterCallback : PagingAdapter.PagingAdapterCallback<RichEpisode>()

    inner class ViewHolder(itemView: View) :
            PagingViewHolder<RichEpisode, EpisodeAdapterCallback>(itemView) {

        override val adapterList: List<RichEpisode>
            get() = list
        override val adapterCallback: EpisodeAdapterCallback?
            get() = callback

        private val number: TextView by bindView(R.id.number)
        private val watched: ImageView by bindView(R.id.watched)
        private val languages: ViewGroup by bindView(R.id.languages)

        override fun bind(item: RichEpisode) {
            number.text = item.number.toString()
            // TODO: watched

            if (languages.childCount < item.languageHosterMap.size) {
                for (i in 0 until item.languageHosterMap.size - languages.childCount) {
                    View.inflate(languages.context, R.layout.layout_episode_language, languages)
                }
            } else if (languages.childCount > item.languageHosterMap.size) {
                for (i in 0 until languages.childCount - item.languageHosterMap.size) {
                    languages.removeViewAt(0)
                }
            }

            var index = 0

            item.languageHosterMap.forEach {
                val language = languages.getChildAt(index).find<TextView>(R.id.language)
                val hosters = languages.getChildAt(index).find<ViewGroup>(R.id.hosters)

                language.text = it.key

                if (it.value == null) {
                    hosters.removeAllViews()
                    hosters.visibility = View.GONE
                } else {
                    hosters.visibility = View.VISIBLE

                    if (hosters.childCount < it.value!!.size) {
                        for (i in 0 until it.value!!.size - hosters.childCount) {
                            View.inflate(hosters.context, R.layout.item_hoster, hosters)
                        }
                    } else if (hosters.childCount > it.value!!.size) {
                        for (i in 0 until hosters.childCount - it.value!!.size) {
                            hosters.removeViewAt(0)
                        }
                    }

                    hosters.forEachChildWithIndex { position, view ->
                        view as ImageView

                        Glide.with(view.context)
                                //   .load(ProxerUrlHolder.getHosterImageUrl(hosters[position].imageId)
                                //             .toString())
                                .load("http://www.flooringvillage.co.uk/ekmps/shops/flooringvillage/images/request-a-sample--547-p.jpg")
                                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                .into(view)
                    }
                }

                index++
            }
        }
    }
}