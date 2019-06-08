package me.proxer.app.auth

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.notification.AccountNotifications
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toLocalSettings
import me.proxer.library.ProxerApi

/**
 * @author Ruben Gees
 */
class LoginHandler(
    private val api: ProxerApi,
    private val storageHelper: StorageHelper,
    private val messengerDao: MessengerDao
) {

    @SuppressLint("CheckResult")
    fun listen(context: Context) {
        storageHelper.isLoggedInObservable
            .subscribe { isLoggedIn ->
                if (isLoggedIn) {
                    onLogin()
                } else {
                    onLogout(context)
                }
            }
    }

    private fun onLogin() {
        MessengerWorker.enqueueSynchronizationIfPossible()
        NotificationWorker.enqueueIfPossible()

        api.ucp.settings()
            .buildSingle()
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors {
                storageHelper.ucpSettings = it.toLocalSettings()
            }
    }

    private fun onLogout(context: Context) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)

        AccountNotifications.cancel(context)
        MessengerNotifications.cancel(context)

        MessengerWorker.cancel()

        Completable
            .fromAction {
                storageHelper.resetUserData()
                messengerDao.clear()
            }
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors()
    }
}
