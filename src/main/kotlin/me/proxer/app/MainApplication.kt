package me.proxer.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.os.Looper
import android.webkit.WebView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.work.Configuration
import androidx.work.WorkManager
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.jakewharton.threetenabp.AndroidThreeTen
import com.kirillr.strictmodehelper.StrictModeCompat
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.rubengees.rxbus.RxBus
import com.squareup.leakcanary.LeakCanary
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import io.reactivex.Completable
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
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.subscribeAndLogErrors
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.startKoin
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.io.File
import java.util.Date
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MainApplication : Application() {

    companion object {
        const val USER_AGENT = "ProxerAndroid/${BuildConfig.VERSION_NAME}"
        const val GENERIC_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"

        var globalContext by Delegates.notNull<Context>()
            private set
    }

    private val bus by inject<RxBus>()
    private val preferenceHelper by inject<PreferenceHelper>()
    private val messengerDao by inject<MessengerDao>()

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }

        LeakCanary.install(this)

        startKoin(this, modules)

        globalContext = this

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

                StorageHelper.lastChatMessageDate = Date(0L)
                StorageHelper.lastNotificationsDate = Date(0L)
                StorageHelper.areConferencesSynchronized = false
                StorageHelper.resetChatInterval()

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
            Timber.plant(FileTree())

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

        DrawerImageLoader.init(ConcreteDrawerImageLoader())
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
                .penaltyLog()
                .build()

            val vmPolicy = StrictModeCompat.VmPolicy.Builder()
                .detectContentUriWithoutPermission()
                .detectLeakedRegistrationObjects()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .detectCleartextNetwork()
                .detectFileUriExposure()
                .detectActivityLeaks()
                .penaltyLog()
                .build()

            StrictModeCompat.setPolicies(threadPolicy, vmPolicy)
        }
    }

    private class FileTree : Timber.Tree() {

        private companion object {
            private val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            Completable
                .fromAction {
                    val logDir = File(globalContext.getExternalFilesDir(null), "logs").also { it.mkdirs() }
                    val logFile = File(logDir, "${LocalDate.now()}.log").also { it.createNewFile() }
                    val currentTime = LocalDateTime.now().format(FORMAT)

                    val maybeTag = if (tag != null) "$tag: " else ""
                    val maybeNewline = if (message.endsWith("\n")) "" else "\n"

                    logFile.appendText("$currentTime  $maybeTag$message$maybeNewline")
                }
                .subscribeOn(Schedulers.io())
                .subscribe()
        }
    }

    private class ConcreteDrawerImageLoader : AbstractDrawerImageLoader() {
        override fun set(image: ImageView, uri: Uri?, placeholder: Drawable?, tag: String?) {
            GlideApp.with(image)
                .load(uri)
                .centerCrop()
                .placeholder(placeholder)
                .into(image)
        }

        override fun cancel(imageView: ImageView) = GlideApp.with(imageView).clear(imageView)

        override fun placeholder(context: Context, tag: String?): IconicsDrawable = IconicsDrawable(context)
            .icon(CommunityMaterial.Icon.cmd_account)
            .colorRes(android.R.color.white)
    }
}
