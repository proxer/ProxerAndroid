package me.proxer.app.chat.prv.conference

import androidx.recyclerview.widget.DiffUtil
import me.proxer.app.chat.prv.ConferenceWithMessage

/**
 * @author Ruben Gees
 */
object ConferenceItemCallback : DiffUtil.ItemCallback<ConferenceWithMessage>() {

    override fun areItemsTheSame(oldItem: ConferenceWithMessage, newItem: ConferenceWithMessage): Boolean {
        return oldItem.conference.id == newItem.conference.id
    }

    override fun areContentsTheSame(oldItem: ConferenceWithMessage, newItem: ConferenceWithMessage): Boolean {
        return oldItem == newItem
    }
}
