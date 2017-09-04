package me.proxer.app.profile.topten

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.profile.topten.TopTenAdapter.ViewHolder
import me.proxer.app.util.extension.defaultLoad
import me.proxer.library.entity.user.TopTenEntry
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class TopTenAdapter : BaseAdapter<TopTenEntry, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, TopTenEntry>> = PublishSubject.create()

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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        glide = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(image to data[it])
                }
            }
        }

        fun bind(item: TopTenEntry) {
            ViewCompat.setTransitionName(image, "top_ten_${item.id}")

            title.text = item.name

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.id))
        }
    }
}
