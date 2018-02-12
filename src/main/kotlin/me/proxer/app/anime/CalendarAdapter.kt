package me.proxer.app.anime

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearLayoutManager.HORIZONTAL
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.anime.CalendarAdapter.ViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.enums.CalendarDay

/**
 * @author Ruben Gees
 */
class CalendarAdapter : BaseAdapter<Pair<CalendarDay, List<CalendarEntry>>, ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, CalendarEntry>> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = data[position].let { (day, _) -> day.ordinal.toLong() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        glide = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val weekDay by bindView<TextView>(R.id.weekDay)
        internal val childRecyclerView by bindView<RecyclerView>(R.id.childRecyclerView)

        private val adapter: CalendarEntryAdapter
            get() = childRecyclerView.adapter as CalendarEntryAdapter

        init {
            val adapter = CalendarEntryAdapter()

            adapter.glide = glide
            adapter.clickSubject.subscribe(clickSubject)

            childRecyclerView.isNestedScrollingEnabled = false
            childRecyclerView.layoutManager = LinearLayoutManager(childRecyclerView.context, HORIZONTAL, false)
            childRecyclerView.adapter = adapter

            GravitySnapHelper(Gravity.START).attachToRecyclerView(childRecyclerView)
        }

        fun bind(item: Pair<CalendarDay, List<CalendarEntry>>) {
            val (day, calendarEntries) = item

            weekDay.text = day.toAppString(weekDay.context)
            adapter.swapDataAndNotifyWithDiffing(calendarEntries)
        }
    }
}
