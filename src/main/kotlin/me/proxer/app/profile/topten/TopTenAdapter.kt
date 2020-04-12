package me.proxer.app.profile.topten

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.profile.topten.TopTenAdapter.ViewHolder
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.mapBindingAdapterPosition
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class TopTenAdapter : BaseAdapter<LocalTopTenEntry, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, LocalTopTenEntry>> = PublishSubject.create()
    val deleteSubject: PublishSubject<LocalTopTenEntry> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_top_ten_entry, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val container: ViewGroup by bindView(R.id.container)
        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)
        internal val deleteButton: ImageButton by bindView(R.id.deleteButton)

        init {
            deleteButton.setIconicsImage(CommunityMaterial.Icon2.cmd_star_off, 48)
        }

        fun bind(item: LocalTopTenEntry) {
            container.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            ViewCompat.setTransitionName(image, "top_ten_${item.id}")

            title.text = item.name

            if (item is LocalTopTenEntry.Ucp) {
                deleteButton.isVisible = true

                deleteButton.clicks()
                    .mapBindingAdapterPosition({ bindingAdapterPosition }) { data[it] }
                    .autoDisposable(this)
                    .subscribe(deleteSubject)

                glide?.defaultLoad(image, ProxerUrls.entryImage(item.entryId))
            } else {
                deleteButton.setOnClickListener(null)
                deleteButton.isVisible = false

                glide?.defaultLoad(image, ProxerUrls.entryImage(item.id))
            }
        }
    }
}
