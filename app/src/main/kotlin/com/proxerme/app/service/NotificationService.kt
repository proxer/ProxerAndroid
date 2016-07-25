package com.proxerme.app.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.support.annotation.StringDef
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.manager.UserManager
import com.proxerme.library.connection.experimental.chat.request.ConferencesRequest
import com.proxerme.library.connection.notifications.request.NewsRequest

/**
 * An IntentService, which retrieves the News and shows a notification if there are unread
 * ones.

 * @author Ruben Gees
 */
class NotificationService : IntentService(NotificationService.SERVICE_TITLE) {

    companion object {

        const val ACTION_LOAD_NEWS = "com.proxerme.app.service.action.LOAD_NEWS"
        const val ACTION_LOAD_CHAT = "com.proxerme.app.service.action.LOAD_CHAT"
        private const val SERVICE_TITLE = "Notification Service"

        fun load(context: Context, @NotificationAction action: String) {
            context.startService(Intent(context, NotificationService::class.java)
                    .apply { this.action = action })
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.run {
            if (ACTION_LOAD_NEWS == action &&
                    SectionManager.currentSection !== SectionManager.Section.NEWS) {
                handleActionLoadNews()
            } else if (ACTION_LOAD_CHAT == action &&
                    SectionManager.currentSection !== SectionManager.Section.CONFERENCES
                    && SectionManager.currentSection !== SectionManager.Section.CHAT) {
                handleActionLoadChat()
            }
        }
    }

    private fun handleActionLoadNews() {
        try {
            val lastTime = StorageHelper.lastNewsTime

            if (lastTime != null) {
                val result = NewsRequest(0).executeSynchronized().item.filter {
                    it.time > lastTime
                }

                if (result.size > StorageHelper.newNews) {
                    NotificationHelper.showNewsNotification(this, result)
                    StorageHelper.newNews = result.size
                }
            }
        } catch (ignored: Exception) {

        }
    }

    private fun handleActionLoadChat() {
        try {
            val lastTime = StorageHelper.lastMessageTime

            if (lastTime != null) {
                UserManager.reLoginSync()

                val result = ConferencesRequest(1).executeSynchronized().item.filter {
                    it.time > lastTime && !it.isRead
                }

                if (result.size > StorageHelper.newMessages) {
                    NotificationHelper.showChatNotification(this, result)
                    StorageHelper.newMessages = result.size
                }
            }
        } catch (ignored: Exception) {

        }

        StorageHelper.incrementChatInterval()
        NotificationHelper.retrieveChatLater(applicationContext)
    }

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(ACTION_LOAD_NEWS, ACTION_LOAD_CHAT)
    annotation class NotificationAction

}
