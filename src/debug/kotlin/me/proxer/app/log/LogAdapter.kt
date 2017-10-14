package me.proxer.app.log

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.log.LogAdapter.ViewHolder
import me.proxer.app.util.Utils
import me.proxer.app.util.data.ParcelableStringBooleanMap

/**
 * @author Ruben Gees
 */
class LogAdapter(savedInstanceState: Bundle?) : BaseAdapter<LogMessage, ViewHolder>() {

    private companion object {
        private const val EXPANDED_STATE = "log_expanded"
    }

    private val expansionMap: ParcelableStringBooleanMap

    init {
        expansionMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false))

    override fun getItemId(position: Int) = itemCount - position.toLong()
    override fun saveInstanceState(outState: Bundle) = outState.putParcelable(EXPANDED_STATE, expansionMap)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val logContent by bindView<TextView>(R.id.logContent)

        init {
            logContent.setOnClickListener {
                withSafeAdapterPosition(this) {
                    val id = getItemId(it).toString()

                    when (expansionMap[id]) {
                        true -> expansionMap.remove(id)
                        else -> expansionMap.put(id, true)
                    }

                    notifyItemChanged(it)
                }
            }
        }

        fun bind(logMessage: LogMessage) {
            if (expansionMap[getItemId(adapterPosition).toString()] == true) {
                logContent.text = logContent.context.getString(R.string.activity_log_expanded_content,
                        Utils.dateTimeFormatter.format(logMessage.dateTime), logMessage.content)

                logContent.maxLines = Int.MAX_VALUE
            } else {
                logContent.text = logMessage.content
                logContent.maxLines = 1
            }
        }
    }
}
