package me.proxer.app.anime

import android.os.Handler
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutParams
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
import me.proxer.app.anime.CalendarEntryAdapter.ViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.defaultLoad
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.util.ProxerUrls
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * @author Ruben Gees
 */
class CalendarEntryAdapter : BaseAdapter<CalendarEntry, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, CalendarEntry>> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_entry, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.handler.removeCallbacks(null)
        holder.updateAction = null

        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        glide = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val image by bindView<ImageView>(R.id.image)
        internal val title by bindView<TextView>(R.id.title)
        internal val episode by bindView<TextView>(R.id.episode)
        internal val ratingContainer by bindView<ViewGroup>(R.id.ratingContainer)
        internal val rating by bindView<RatingBar>(R.id.rating)
        internal val airingInfo by bindView<TextView>(R.id.airingInfo)
        internal val status by bindView<TextView>(R.id.status)

        internal val handler = Handler()
        internal var updateAction: Runnable? = null

        init {
            val width = DeviceUtils.getScreenWidth(itemView.context) / when {
                DeviceUtils.isLandscape(itemView.resources) -> 4.5
                else -> 2.5
            }

            itemView.layoutParams = LayoutParams(width.toInt(), LayoutParams.WRAP_CONTENT)

            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    clickSubject.onNext(image to data[it])
                }
            }
        }

        fun bind(item: CalendarEntry) {
            ViewCompat.setTransitionName(image, "calendar_${item.id}")

            title.text = item.name
            episode.text = episode.context.getString(R.string.calendar_episode, item.episode.toString())

            if (item.rating > 0) {
                ratingContainer.visibility = View.VISIBLE
                rating.rating = item.rating / 2.0f
            } else {
                ratingContainer.visibility = View.GONE
            }

            val convertedDate = item.date.convertToDateTime()
            val convertedUploadDate = item.uploadDate.convertToDateTime()

            val airingDate = DateTimeFormatter.ofPattern("HH:mm").format(convertedDate)
            val uploadDate = DateTimeFormatter.ofPattern("HH:mm").format(convertedUploadDate)

            airingInfo.text = "Ausstrahlung:\n${airingDate}\nAuf Proxer (geschätzt):\n${uploadDate}"

            updateAction = Runnable {
                val now = LocalDateTime.now()

                if (convertedUploadDate.isBefore(now)) {
                    status.text = "Auf Proxer (geschätzt)"
                } else {
                    val duration = Duration.between(now, convertedUploadDate)

                    val days = duration.toDays()
                    val hours = duration.minusDays(days).toHours()
                    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
                    val seconds = duration.minusDays(days).minusHours(hours).minusMinutes(minutes).seconds

                    val formattedDuration = "%02d:%02d:%02d:%02d".format(days, hours, minutes, seconds)

                    if (convertedDate.isBefore(now)) {
                        status.text = "Ausgestrahlt\nNoch $formattedDuration"
                    } else {
                        status.text = "Noch $formattedDuration"
                    }
                }

                handler.postDelayed(updateAction, 1000)
            }

            handler.post(updateAction)
            glide?.defaultLoad(image, ProxerUrls.entryImage(item.entryId))
        }
    }
}
