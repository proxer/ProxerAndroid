package me.proxer.app.media.episode

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.R
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.media.episode.EpisodeAdapter.ViewHolder
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.getSafeParcelable
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.dip
import org.jetbrains.anko.forEachChildWithIndex

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
    private var isLoggedIn = StorageHelper.isLoggedIn

    private var busDisposable: Disposable? = null

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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        busDisposable = Observable.merge(bus.register(LoginEvent::class.java), bus.register(LogoutEvent::class.java))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                isLoggedIn = when (it) {
                    is LoginEvent -> true
                    is LogoutEvent -> false
                    else -> false
                }

                notifyDataSetChanged()
            }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        busDisposable?.dispose()
        busDisposable = null
        glide = null
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.languages.applyRecursively {
            if (it is ImageView) {
                glide?.clear(it)
            }
        }
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

            if (item.userProgress >= item.number) {
                watched.visibility = View.VISIBLE
            } else {
                watched.visibility = View.INVISIBLE
            }

            if (expansionMap.containsKey(item.number.toString())) {
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
                val languageView = languageContainer.findViewById<TextView>(R.id.language)
                val hostersView = languageContainer.findViewById<ViewGroup>(R.id.hosters)

                languageView.text = language.toAppString(languageView.context)
                languageView.setCompoundDrawablesWithIntrinsicBounds(language.toGeneralLanguage()
                    .toAppDrawable(languageView.context), null, null, null)

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

                hostersView.visibility = View.GONE
            } else {
                hostersView.visibility = View.VISIBLE

                if (hostersView.childCount != hosterImages.size) {
                    hostersView.removeAllViews()

                    for (i in 0 until hosterImages.size) {
                        val inflater = LayoutInflater.from(hostersView.context)
                        val imageView = inflater.inflate(R.layout.layout_image, hostersView, false).apply {
                            layoutParams.width = dip(28)
                            layoutParams.height = dip(28)
                        }

                        hostersView.addView(imageView)
                    }
                }

                hostersView.forEachChildWithIndex { index, imageView ->
                    glide?.defaultLoad(imageView as ImageView, ProxerUrls.hosterImage(hosterImages[index]))
                }
            }
        }
    }
}
