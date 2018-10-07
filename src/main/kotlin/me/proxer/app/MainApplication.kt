package me.proxer.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.os.Looper
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.work.Configuration
import androidx.work.WorkManager
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.jakewharton.threetenabp.AndroidThreeTen
import com.kirillr.strictmodehelper.StrictModeCompat
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.rubengees.rxbus.RxBus
import com.squareup.leakcanary.LeakCanary
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.notification.AccountNotifications
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.util.GlideDrawerImageLoader
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.TimberFileTree
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.subscribeAndLogErrors
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.startKoin
import timber.log.Timber
import java.util.Date

/**
 * @author Ruben Gees
 */
class MainApplication : Application() {

    companion object {
        const val USER_AGENT = "ProxerAndroid/${BuildConfig.VERSION_NAME}"
        const val GENERIC_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    }

    private val bus by inject<RxBus>()
    private val storageHelper by inject<StorageHelper>()
    private val preferenceHelper by inject<PreferenceHelper>()
    private val messengerDao by inject<MessengerDao>()

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }

        LeakCanary.install(this)

        startKoin(this, modules)

        NotificationUtils.createNotificationChannels(this)

        initBus()
        initLibs()
        initCache()
        initNightMode()
        enableStrictModeForDebug()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        if (BuildConfig.DEBUG) {
            MultiDex.install(this)
        }
    }

    private fun initNightMode() {
        val nightMode = preferenceHelper.nightMode

        // Ugly hack to avoid WebViews to change the ui mode. On first inflation, a WebView changes the ui mode
        // and creating an instance before the first inflation fixes that.
        // See: https://issuetracker.google.com/issues/37124582
        if (nightMode != AppCompatDelegate.MODE_NIGHT_NO) {
            try {
                WebView(this)
            } catch (ignored: Throwable) {
            }
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun initBus() {
        bus.register(LoginEvent::class.java)
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors {
                MessengerWorker.enqueueSynchronizationIfPossible()
                NotificationWorker.enqueueIfPossible()
            }

        bus.register(LogoutEvent::class.java)
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors {
                AccountNotifications.cancel(this)
                MessengerNotifications.cancel(this)

                MessengerWorker.cancel()

                storageHelper.lastChatMessageDate = Date(0L)
                storageHelper.lastNotificationsDate = Date(0L)
                storageHelper.areConferencesSynchronized = false
                storageHelper.resetChatInterval()

                messengerDao.clear()
            }
    }

    private fun initLibs() {
        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_CRASH)
            .apply()

        EmojiManager.install(IosEmojiProvider())
        AndroidThreeTen.init(this)

        WorkManager.initialize(this, Configuration.Builder().build())

        if (BuildConfig.LOG) {
            Timber.plant(TimberFileTree(this))

            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
        }

        RxJavaPlugins.setErrorHandler { error ->
            when (error) {
                is UndeliverableException -> Timber.w("Can't deliver error: $error")
                is InterruptedException -> Timber.w(error)
                else -> Thread.currentThread().uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), error)
            }
        }

        RxAndroidPlugins.setInitMainThreadSchedulerHandler { AndroidSchedulers.from(Looper.getMainLooper(), true) }
        RxAndroidPlugins.setMainThreadSchedulerHandler { AndroidSchedulers.from(Looper.getMainLooper(), true) }

        DrawerImageLoader.init(GlideDrawerImageLoader())
        SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.RGB_565)
    }

    private fun initCache() {
        val hasExternalStorage = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

        if (!preferenceHelper.isCacheExternallySet) {
            preferenceHelper.shouldCacheExternally = hasExternalStorage
        } else if (preferenceHelper.shouldCacheExternally && !hasExternalStorage) {
            preferenceHelper.shouldCacheExternally = false
        }
    }

    private fun enableStrictModeForDebug() {
        if (BuildConfig.DEBUG) {
            val threadPolicy = StrictModeCompat.ThreadPolicy.Builder()
                .detectAll()
                .permitDiskReads()
                .penaltyDeath()
                .penaltyLog()
                .build()

            val vmPolicy = StrictModeCompat.VmPolicy.Builder()
                .detectAll()
                .penaltyDeath()
                .penaltyLog()
                .build()

            StrictModeCompat.setPolicies(threadPolicy, vmPolicy)
        }
    }
}
