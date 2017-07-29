package me.proxer.app.info

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.info.TranslatorGroupProjectAdapter.ViewHolder
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entitiy.list.TranslatorGroupProject
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class TranslatorGroupProjectAdapter(private val glide: GlideRequests)
    : BaseAdapter<TranslatorGroupProject, ViewHolder>() {

    val clickSubject: PublishSubject<Pair<ImageView, TranslatorGroupProject>> =
            PublishSubject.create<Pair<ImageView, TranslatorGroupProject>>()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])
    override fun onViewRecycled(holder: ViewHolder) = glide.clear(holder.image)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val medium: TextView by bindView(R.id.medium)
        internal val image: ImageView by bindView(R.id.image)
        internal val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        internal val rating: RatingBar by bindView(R.id.rating)
        internal val status: TextView by bindView(R.id.status)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(image to data[it])
                }
            }
        }

        fun bind(item: TranslatorGroupProject) {
            ViewCompat.setTransitionName(image, "translator_group_project_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            status.text = item.state.toAppString(status.context)

            if (item.rating > 0) {
                ratingContainer.visibility = View.VISIBLE
                rating.rating = item.rating / 2.0f
            } else {
                ratingContainer.visibility = View.GONE
            }

            glide.defaultLoad(image, ProxerUrls.entryImage(item.id))
        }
    }
}
