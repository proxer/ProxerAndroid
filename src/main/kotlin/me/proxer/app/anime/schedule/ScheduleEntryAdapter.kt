package me.proxer.app.anime.schedule

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.set
import androidx.core.view.ViewCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.anime.schedule.ScheduleEntryAdapter.ViewHolder
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.formattedDistanceTo
import me.proxer.app.util.extension.mapBindingAdapterPosition
import me.proxer.app.util.extension.toLocalDateTimeBP
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.util.ProxerUrls
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.Collections
import java.util.Locale
import java.util.WeakHashMap
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class ScheduleEntryAdapter : BaseAdapter<CalendarEntry, ViewHolder>() {

    private companion object {
        private val hourMinuteDateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val dayTextDateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN)
    }

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, CalendarEntry>> = PublishSubject.create()

    private var layoutManager: RecyclerView.LayoutManager? = null

    private var currentMinAiringInfoLines = 2
    private var currentMinStatusLines = 2
    private val cachedViewHolders = Collections.newSetFromMap(WeakHashMap<ViewHolder, Boolean>())

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_schedule_entry, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])

        cachedViewHolders += holder
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        layoutManager = recyclerView.layoutManager
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.itemView.post {
            if (holder.airingInfo.lineCount > currentMinAiringInfoLines) {
                currentMinAiringInfoLines = holder.airingInfo.lineCount

                cachedViewHolders.forEach { it.airingInfo.minLines = currentMinAiringInfoLines }

                layoutManager?.requestSimpleAnimationsInNextLayout()
            }

            if (holder.status.lineCount > currentMinStatusLines) {
                currentMinStatusLines = holder.status.lineCount

                cachedViewHolders.forEach { it.status.minLines = currentMinStatusLines }

                layoutManager?.requestSimpleAnimationsInNextLayout()
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.airingInfoDisposable?.dispose()
        holder.airingInfoDisposable = null

        glide?.clear(holder.image)

        cachedViewHolders -= holder
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        layoutManager = null
        glide = null
    }

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val container: ViewGroup by bindView(R.id.container)
        internal val image by bindView<ImageView>(R.id.image)
        internal val title by bindView<TextView>(R.id.title)
        internal val episode by bindView<TextView>(R.id.episode)
        internal val ratingContainer by bindView<ViewGroup>(R.id.ratingContainer)
        internal val rating by bindView<RatingBar>(R.id.rating)
        internal val airingInfo by bindView<TextView>(R.id.airingInfo)
        internal val status by bindView<TextView>(R.id.status)

        internal var airingInfoDisposable: Disposable? = null

        init {
            val itemsPerPage = when {
                DeviceUtils.isLargeTablet(itemView.context) -> when (DeviceUtils.isLandscape(itemView.resources)) {
                    true -> 6.5f
                    false -> 4.5f
                }
                DeviceUtils.isTablet(itemView.context) -> when (DeviceUtils.isLandscape(itemView.resources)) {
                    true -> 5f
                    false -> 3f
                }
                else -> when (DeviceUtils.isLandscape(itemView.resources)) {
                    true -> 4.5f
                    false -> 2.25f
                }
            }

            val margin = itemView.context.resources.getDimension(R.dimen.screen_horizontal_margin)
            val width = (DeviceUtils.getScreenWidth(itemView.context) - margin) / itemsPerPage

            itemView.layoutParams = RecyclerView.LayoutParams(width.toInt(), RecyclerView.LayoutParams.WRAP_CONTENT)
        }

        fun bind(item: CalendarEntry) {
            container.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            ViewCompat.setTransitionName(image, "schedule_${item.id}")

            title.text = item.name
            episode.text = episode.context.getString(R.string.fragment_schedule_episode, item.episode.toString())

            bindRating(item)
            bindAiringInfo(item)

            airingInfo.minLines = currentMinAiringInfoLines
            status.minLines = currentMinStatusLines

            airingInfoDisposable?.dispose()
            airingInfoDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(this)
                .subscribe(AiringInfoUpdateConsumer(item))

            glide?.defaultLoad(image, ProxerUrls.entryImage(item.entryId))
        }

        private fun bindRating(item: CalendarEntry) {
            if (item.rating > 0) {
                ratingContainer.isVisible = true
                rating.rating = item.rating / 2.0f

                airingInfo.updateLayoutParams<RelativeLayout.LayoutParams> {
                    bottomMargin = 0

                    addRule(RelativeLayout.BELOW, R.id.ratingContainer)
                }
            } else {
                ratingContainer.isInvisible = true

                ratingContainer.measure(
                    makeMeasureSpec(DeviceUtils.getScreenWidth(ratingContainer.context), AT_MOST),
                    makeMeasureSpec(0, UNSPECIFIED)
                )

                airingInfo.updateLayoutParams<RelativeLayout.LayoutParams> {
                    val containerMargin = ratingContainer.marginTop

                    bottomMargin = ratingContainer.measuredHeight + containerMargin

                    addRule(RelativeLayout.BELOW, R.id.image)
                }
            }
        }

        private fun bindAiringInfo(item: CalendarEntry) {
            val itemDateTime = item.date.toLocalDateTimeBP()
            val itemUploadDateTime = item.uploadDate.toLocalDateTimeBP()

            val airingDateText = hourMinuteDateTimeFormatter.format(itemDateTime)
            val uploadDate = hourMinuteDateTimeFormatter.format(itemUploadDateTime)

            val uploadDateText = when (itemUploadDateTime.toLocalDate() != itemDateTime.toLocalDate()) {
                true -> itemUploadDateTime.format(dayTextDateTimeFormatter) + ", " + uploadDate
                false -> uploadDate
            }

            if (item.date == item.uploadDate) {
                val airingText = airingInfo.context.getString(R.string.fragment_schedule_airing, airingDateText)

                airingInfo.text = SpannableString(airingText).apply {
                    this[indexOf(airingDateText)..length] = StyleSpan(Typeface.BOLD)
                }
            } else {
                val airingUploadText = airingInfo.context.getString(
                    R.string.fragment_schedule_airing_upload,
                    airingDateText,
                    uploadDateText
                )

                airingInfo.text = SpannableString(airingUploadText).apply {
                    this[indexOf(airingDateText)..indexOf("\n")] = StyleSpan(Typeface.BOLD)
                    this[lastIndexOf(uploadDateText)..length] = StyleSpan(Typeface.BOLD)
                }
            }
        }

        private inner class AiringInfoUpdateConsumer(private val item: CalendarEntry) : Consumer<Long> {

            override fun accept(t: Long?) {
                val now = LocalDateTime.now()

                if (item.uploadDate.toLocalDateTimeBP().isBefore(now)) {
                    if (item.date == item.uploadDate) {
                        val airedText = status.context.getString(R.string.fragment_schedule_aired)

                        status.text = SpannableString(airedText).apply {
                            val span = ForegroundColorSpan(ContextCompat.getColor(status.context, R.color.green_500))

                            this[0..length] = span
                        }
                    } else {
                        val uploadedText = status.context.getString(R.string.fragment_schedule_uploaded)

                        status.text = SpannableString(uploadedText).apply {
                            val span = ForegroundColorSpan(ContextCompat.getColor(status.context, R.color.green_500))

                            this[0..length] = span
                        }
                    }
                } else {
                    if (item.date.toLocalDateTimeBP().isBefore(now)) {
                        status.text = status.context.getString(
                            R.string.fragment_schedule_aired_remaining_time,
                            LocalDateTime.now().formattedDistanceTo(item.uploadDate.toLocalDateTimeBP())
                        )
                    } else {
                        status.text = status.context.getString(
                            R.string.fragment_schedule_remaining_time,
                            LocalDateTime.now().formattedDistanceTo(item.date.toLocalDateTimeBP())
                        )
                    }
                }
            }
        }
    }
}
