package com.proxerme.app.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.proxerme.app.application.MainApplication
import com.proxerme.app.event.NewsEvent
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.manager.SectionManager
import com.proxerme.library.connection.notifications.request.NewsRequest
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.intentFor

/**
 * An IntentService, which retrieves the News and shows a notification if there are unread
 * ones.

 * @author Ruben Gees
 */
class NotificationService : IntentService(NotificationService.SERVICE_TITLE) {

    companion object {
        const val ACTION_LOAD_NEWS = "com.proxerme.app.service.action.LOAD_NEWS"
        private const val SERVICE_TITLE = "Notification Service"

        fun load(context: Context, action: String) {
            context.startService(context.intentFor<NotificationService>().setAction(action))
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.run {
            if (ACTION_LOAD_NEWS == action) {
                handleActionLoadNews()
            }
        }
    }

    private fun handleActionLoadNews() {
        try {
            val lastTime = StorageHelper.lastNewsTime

            if (lastTime != null) {
                val result = MainApplication.proxerConnection.executeSynchronized(NewsRequest(0))
                        .filter {
                            it.time > lastTime
                        }

                if (result.size > StorageHelper.newNews) {
                    if (SectionManager.currentSection == SectionManager.Section.NEWS) {
                        EventBus.getDefault().post(NewsEvent(result))
                    } else {
                        NotificationHelper.showNewsNotification(this, result)
                        StorageHelper.newNews = result.size
                    }
                }
            }
        } catch (ignored: Exception) {

        }
    }
}
