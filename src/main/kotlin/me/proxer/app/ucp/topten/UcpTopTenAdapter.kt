package me.proxer.app.ucp.topten

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.ucp.topten.UcpTopTenAdapter.ViewHolder
import me.proxer.app.util.extension.defaultLoad
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val image: ImageView by bindView(R.id.image)
        internal val removeButton: ImageButton by bindView(R.id.deleteButton)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(image to data[it])
                }
            }

            removeButton.setIconicsImage(CommunityMaterial.Icon.cmd_star_off, 48)

            removeButton.setOnClickListener {
                withSafeAdapterPosition(this) {
                    deleteSubject.onNext(data[it])
                }
            }
        }

        fun bind(item: UcpTopTenEntry) {
            ViewCompat.setTransitionName(image, "ucp_top_ten_${item.id}")

            title.text = item.name

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.entryId))
        }
    }
}
