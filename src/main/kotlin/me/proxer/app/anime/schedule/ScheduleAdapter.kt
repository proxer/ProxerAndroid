package me.proxer.app.anime.schedule

import android.os.Parcelable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.anime.schedule.ScheduleAdapter.ViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.safeLayoutManager
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.enums.CalendarDay
import kotlin.math.max

/**
 * @author Ruben Gees
 */
class ScheduleAdapter : BaseAdapter<Pair<CalendarDay, List<CalendarEntry>>, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, CalendarEntry>> = PublishSubject.create()

    private val layoutManagerStates = mutableMapOf<CalendarDay, Parcelable?>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = data[position].let { (day, _) -> day.ordinal.toLong() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_schedule_day, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun onViewRecycled(holder: ViewHolder) {
        withSafeBindingAdapterPosition(holder) {
            layoutManagerStates[data[it].first] = holder.childRecyclerView.safeLayoutManager.onSaveInstanceState()
        }

        holder.childRecyclerView.layoutManager = null
        holder.childRecyclerView.adapter = null
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val weekDay by bindView<TextView>(R.id.weekDay)
        internal val childRecyclerView by bindView<RecyclerView>(R.id.childRecyclerView)

        init {
            childRecyclerView.setHasFixedSize(true)
            childRecyclerView.isNestedScrollingEnabled = false

            GravitySnapHelper(Gravity.START).attachToRecyclerView(childRecyclerView)
        }

        fun bind(item: Pair<CalendarDay, List<CalendarEntry>>) {
            val (day, calendarEntries) = item
            val adapter = ScheduleEntryAdapter()

            adapter.glide = glide
            adapter.clickSubject.subscribe(clickSubject)
            adapter.swapDataAndNotifyWithDiffing(calendarEntries)

            weekDay.text = day.toAppString(weekDay.context)

            childRecyclerView.layoutManager = LinearLayoutManager(childRecyclerView.context, HORIZONTAL, false).apply {
                initialPrefetchItemCount = when (DeviceUtils.isLandscape(itemView.resources)) {
                    true -> max(5, calendarEntries.size)
                    false -> max(3, calendarEntries.size)
                }
            }

            childRecyclerView.swapAdapter(adapter, false)

            layoutManagerStates[item.first]?.let {
                childRecyclerView.safeLayoutManager.onRestoreInstanceState(it)
            }
        }
    }
}
