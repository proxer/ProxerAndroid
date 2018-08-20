package me.proxer.app.ucp.topten

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.ucp.topten.UcpTopTenAdapter.ViewHolder
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.library.entity.ucp.UcpTopTenEntry
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class UcpTopTenAdapter : BaseAdapter<UcpTopTenEntry, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, UcpTopTenEntry>> = PublishSubject.create()
    val deleteSubject: PublishSubject<UcpTopTenEntry> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_ucp_top_ten_entry, parent, false))
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
        internal val image: ImageView by bindView(R.id.image)
        internal val removeButton: ImageButton by bindView(R.id.deleteButton)

        init {
            removeButton.setIconicsImage(CommunityMaterial.Icon.cmd_star_off, 48)
        }

        fun bind(item: UcpTopTenEntry) {
            itemView.clicks()
                .mapAdapterPosition({ adapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            removeButton.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe(deleteSubject)

            ViewCompat.setTransitionName(image, "ucp_top_ten_${item.id}")

            title.text = item.name

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.entryId))
        }
    }
}
