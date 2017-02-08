package com.proxerme.app.helper

import com.orhanobut.hawk.Hawk
import com.proxerme.app.entitiy.LocalUser
import java.util.*

/**
 * A helper class, giving access to the storage.

 * @author Ruben Gees
 */
object StorageHelper {

    const private val FIRST_START = "first_start"
    const private val USER = "user"
    const private val NEWS_LAST_TIME = "news_last_time"
    const private val NEW_NEWS = "new_news"
    const private val CONFERENCE_LIST_END_REACHED = "conference_list_end_reached"
    const private val CONFERENCE_END_REACHED_MAP = "conference_end_reached_map"
    const private val CHAT_NOTIFICATIONS_INTERVAL = "chat_notifications_interval"

    const private val MAX_CHAT_POLLING_INTERVAL = 850L
    const private val DEFAULT_CHAT_INTERVAL = 5L
    const private val CHAT_INTERVAL_MULTIPLICAND = 1.5

    var firstStart: Boolean
        get() = Hawk.get(FIRST_START, true)
        set(firstStart) {
            Hawk.put(FIRST_START, false)
        }

    var user: LocalUser?
        get() = Hawk.get(USER)
        set(value) {
            if (value == null) {
                Hawk.delete(USER)
            } else {
                Hawk.put(USER, value)
            }
        }

    var chatInterval: Long
        get() = Hawk.get(CHAT_NOTIFICATIONS_INTERVAL, DEFAULT_CHAT_INTERVAL)
        set(chatInterval) {
            Hawk.put(CHAT_NOTIFICATIONS_INTERVAL, chatInterval)
        }

    var lastNewsTime: Long?
        get() = Hawk.get(NEWS_LAST_TIME, null)
        set(lastNewsTime) {
            if (lastNewsTime == null) {
                Hawk.delete(NEWS_LAST_TIME)
            } else {
                Hawk.put(NEWS_LAST_TIME, lastNewsTime)
            }
        }

    var newNews: Int
        get() = Hawk.get(NEW_NEWS, 0)
        set(newNews) {
            Hawk.put(NEW_NEWS, newNews)
        }

    var conferenceListEndReached: Boolean
        get() = Hawk.get(CONFERENCE_LIST_END_REACHED, false)
        set(hasReachedEnd) {
            Hawk.put(CONFERENCE_LIST_END_REACHED, hasReachedEnd)
        }

    fun hasConferenceReachedEnd(conferenceId: String): Boolean {
        return Hawk.get<Map<String, Boolean>>(CONFERENCE_END_REACHED_MAP, HashMap())
                .getOrElse(conferenceId, { false })
    }

    fun setConferenceReachedEnd(conferenceId: String) {
        val map: MutableMap<String, Boolean> = Hawk.get(CONFERENCE_END_REACHED_MAP,
                HashMap<String, Boolean>())

        map.put(conferenceId, true)

        Hawk.put(CONFERENCE_END_REACHED_MAP, map)
    }

    fun resetConferenceReachedEndMap() {
        Hawk.put(CONFERENCE_END_REACHED_MAP, HashMap<String, Boolean>())
    }

    fun incrementChatInterval() {
        val interval = chatInterval

        if (interval <= MAX_CHAT_POLLING_INTERVAL) {
            Hawk.put(CHAT_NOTIFICATIONS_INTERVAL, interval * CHAT_INTERVAL_MULTIPLICAND)
        }
    }

    fun resetChatInterval() {
        Hawk.put(CHAT_NOTIFICATIONS_INTERVAL, DEFAULT_CHAT_INTERVAL)
    }
}
