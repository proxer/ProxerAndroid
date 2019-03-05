package me.proxer.app.bookmark

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import com.mikepenz.iconics.typeface.library.communitymaterial.CommunityMaterial
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.entity.ucp.Bookmark
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class BookmarkAdapter : BaseAdapter<Bookmark, BookmarkAdapter.ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Bookmark> = PublishSubject.create()
    val longClickSubject: PublishSubject<Pair<ImageView, Bookmark>> = PublishSubject.create()
    val deleteClickSubject: PublishSubject<Bookmark> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    override fun areItemsTheSame(old: Bookmark, new: Bookmark) = old.entryId == new.entryId
    override fun areContentsTheSame(old: Bookmark, new: Bookmark) = old.id == new.id

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val container: ViewGroup by bindView(R.id.container)
        internal val title: TextView by bindView(R.id.title)
        internal val medium: TextView by bindView(R.id.medium)
        internal val image: ImageView by bindView(R.id.image)
        internal val episode: TextView by bindView(R.id.episode)
        internal val language: ImageView by bindView(R.id.language)
        internal val delete: ImageButton by bindView(R.id.delete)

        init {
            delete.setIconicsImage(CommunityMaterial.Icon.cmd_bookmark_remove, 48)
        }

        fun bind(item: Bookmark) {
            initListeners()

            ViewCompat.setTransitionName(image, "bookmark_${item.id}")

            val availabilityIndicator = AppCompatResources.getDrawable(
                episode.context,
                when (item.isAvailable) {
                    true -> R.drawable.ic_circle_green
                    false -> R.drawable.ic_circle_red
                }
            )

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            episode.text = item.chapterName ?: item.category.toEpisodeAppString(episode.context, item.episode)

            episode.setCompoundDrawablesWithIntrinsicBounds(null, null, availabilityIndicator, null)
            language.setImageDrawable(item.language.toGeneralLanguage().toAppDrawable(language.context))

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.entryId))
        }

        private fun initListeners() {
            container.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            container.longClicks()
                .mapAdapterPosition({ adapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(longClickSubject)

            delete.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe(deleteClickSubject)
        }
    }
}
