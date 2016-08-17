package com.proxerme.app.helper

import android.support.annotation.IntRange
import com.orhanobut.hawk.Hawk
import com.proxerme.library.connection.user.entitiy.User
import java.util.*

/**
 * A helper class, giving access to the storage.

 * @author Ruben Gees
 */
object StorageHelper {

    const private val STORAGE_CHAT_NOTIFICATIONS_INTERVAL = "storage_chat_notifications_interval"
    const private val STORAGE_USER_IMAGE_ID = "storage_user_image_id"
    const private val STORAGE_FIRST_START = "storage_first_start"
    const private val STORAGE_NEWS_LAST_TIME = "storage_news_last_time"
    const private val STORAGE_USER_USERNAME = "storage_user_username"
    const private val STORAGE_USER_PASSWORD = "storage_user_password"
    const private val STORAGE_USER_ID = "storage_user_id"
    const private val STORAGE_LAST_LOGIN_TIME = "storage_last_login"
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

    var user: User?
        get() {
            val username: String? = Hawk.get(STORAGE_USER_USERNAME)
            val password: String? = Hawk.get(STORAGE_USER_PASSWORD)
            val id: String? = Hawk.get(STORAGE_USER_ID)
            val imageId: String? = Hawk.get(STORAGE_USER_IMAGE_ID)

            if (username == null || password == null || id == null || imageId == null) {
                return null
            } else {
                return User(username, password, id, imageId)
            }
        }
        set(value) {
            if (value == null) {
                Hawk.remove(STORAGE_USER_USERNAME, STORAGE_USER_PASSWORD, STORAGE_USER_ID,
                        STORAGE_USER_IMAGE_ID)
            } else {
                Hawk.chain(4).put(STORAGE_USER_USERNAME,
                        value.username).put(STORAGE_USER_PASSWORD,
                        value.password).put(STORAGE_USER_ID,
                        value.id).put(STORAGE_USER_IMAGE_ID,
                        value.imageId).commit()
            }
        }

    var chatInterval: Long
        @IntRange(from = 5)
        get() = Hawk.get(STORAGE_CHAT_NOTIFICATIONS_INTERVAL, DEFAULT_CHAT_INTERVAL)
        set(chatInterval) {
            Hawk.put(STORAGE_CHAT_NOTIFICATIONS_INTERVAL, chatInterval)
        }

    var lastLoginTime: Long?
        get() = Hawk.get(STORAGE_LAST_LOGIN_TIME, null)
        set(lastLoginTime) {
            if (lastLoginTime == null) {
                Hawk.remove(STORAGE_LAST_LOGIN_TIME)
            } else {
                Hawk.put(STORAGE_LAST_LOGIN_TIME, lastLoginTime)
            }
        }

    var lastNewsTime: Long?
        get() = Hawk.get(STORAGE_NEWS_LAST_TIME, null)
        set(lastNewsTime) {
            if (lastNewsTime == null) {
                Hawk.remove(STORAGE_NEWS_LAST_TIME)
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
                HashMap())

        map.put(conferenceId, true)

        Hawk.put(STORAGE_CONFERENCE_END_REACHED_MAP, map)
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
