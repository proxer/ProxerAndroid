package com.proxerme.app.helper

import com.orhanobut.hawk.Hawk
import com.proxerme.app.entitiy.LocalUser
import java.util.*

/**
 * A helper class, giving access to the storage.

 * @author Ruben Gees
 */
object StorageHelper {

    const private val STORAGE_CHAT_NOTIFICATIONS_INTERVAL = "storage_chat_notifications_interval"
    const private val STORAGE_FIRST_START = "storage_first_start"
    const private val STORAGE_USER = "storage_user"
    const private val STORAGE_NEWS_LAST_TIME = "storage_news_last_time"
    const private val STORAGE_NEW_NEWS = "storage_new_news"
    const private val STORAGE_CONFERENCE_LIST_END_REACHED = "storage_conference_list_end_reached"
    const private val STORAGE_CONFERENCE_END_REACHED_MAP = "storage_conference_end_reached_map"

    const private val MAX_CHAT_POLLING_INTERVAL = 850L
    const private val DEFAULT_CHAT_INTERVAL = 5L
    const private val CHAT_INTERVAL_MULTIPLICAND = 1.5

    var firstStart: Boolean
        get() = Hawk.get(STORAGE_FIRST_START, true)
        set(firstStart) {
            Hawk.put(STORAGE_FIRST_START, false)
        }

    var user: LocalUser?
        get() = Hawk.get(STORAGE_USER)
        set(value) {
            if (value == null) {
                Hawk.delete(STORAGE_USER)
            } else {
                Hawk.put(STORAGE_USER, value)
            }
        }

    var chatInterval: Long
        get() = Hawk.get(STORAGE_CHAT_NOTIFICATIONS_INTERVAL, DEFAULT_CHAT_INTERVAL)
        set(chatInterval) {
            Hawk.put(STORAGE_CHAT_NOTIFICATIONS_INTERVAL, chatInterval)
        }

    var lastNewsTime: Long?
        get() = Hawk.get(STORAGE_NEWS_LAST_TIME, null)
        set(lastNewsTime) {
            if (lastNewsTime == null) {
                Hawk.delete(STORAGE_NEWS_LAST_TIME)
            } else {
                Hawk.put(STORAGE_NEWS_LAST_TIME, lastNewsTime)
            }
        }

    var newNews: Int
        get() = Hawk.get(STORAGE_NEW_NEWS, 0)
        set(newNews) {
            Hawk.put(STORAGE_NEW_NEWS, newNews)
        }

    var conferenceListEndReached: Boolean
        get() = Hawk.get(STORAGE_CONFERENCE_LIST_END_REACHED, false)
        set(hasReachedEnd) {
            Hawk.put(STORAGE_CONFERENCE_LIST_END_REACHED, hasReachedEnd)
        }

    fun hasConferenceReachedEnd(conferenceId: String): Boolean {
        return Hawk.get<Map<String, Boolean>>(STORAGE_CONFERENCE_END_REACHED_MAP, HashMap())
                .getOrElse(conferenceId, { false })
    }

    fun setConferenceReachedEnd(conferenceId: String) {
        val map: MutableMap<String, Boolean> = Hawk.get(STORAGE_CONFERENCE_END_REACHED_MAP,
                HashMap<String, Boolean>())

        map.put(conferenceId, true)

        Hawk.put(STORAGE_CONFERENCE_END_REACHED_MAP, map)
    }

    fun resetConferenceReachedEndMap() {
        Hawk.put(STORAGE_CONFERENCE_END_REACHED_MAP, HashMap<String, Boolean>())
    }

    fun incrementChatInterval() {
        val interval = chatInterval

        if (interval <= MAX_CHAT_POLLING_INTERVAL) {
            Hawk.put(STORAGE_CHAT_NOTIFICATIONS_INTERVAL, interval * CHAT_INTERVAL_MULTIPLICAND)
        }
    }

    fun resetChatInterval() {
        Hawk.put(STORAGE_CHAT_NOTIFICATIONS_INTERVAL, DEFAULT_CHAT_INTERVAL)
    }
}
