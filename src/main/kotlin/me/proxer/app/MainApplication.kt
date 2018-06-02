package me.proxer.app

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.multidex.MultiDex
import android.support.v7.app.AppCompatDelegate
import android.util.Log
import android.webkit.WebView
import android.widget.ImageView
import androidx.work.Configuration
import androidx.work.WorkManager
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.devbrackets.android.exomedia.ExoMedia
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.jakewharton.threetenabp.AndroidThreeTen
import com.kirillr.strictmodehelper.StrictModeCompat
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.rubengees.rxbus.RxBus
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.squareup.moshi.Moshi
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.auth.ProxerLoginTokenManager
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerDatabase
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.media.TagDao
import me.proxer.app.media.TagDatabase
import me.proxer.app.notification.AccountNotifications
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.api.ProxerApi
import me.proxer.library.api.ProxerApi.Builder.LoggingStrategy
import okhttp3.OkHttpClient
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MainApplication : Application() {

    companion object {
        const val LOGGING_TAG = "ProxerAndroid"
        const val USER_AGENT = "ProxerAndroid/${BuildConfig.VERSION_NAME}"
        const val GENERIC_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"

        val bus = RxBus()

        val moshi: Moshi
            get() = api.moshi()

        val client: OkHttpClient
            get() = api.client()

        val messengerDao: MessengerDao
            get() = messengerDatabase.dao()

        val tagDao: TagDao
            get() = tagDatabase.dao()

        var api by Delegates.notNull<ProxerApi>()
            private set

        var messengerDatabase by Delegates.notNull<MessengerDatabase>()
            private set

        var tagDatabase by Delegates.notNull<TagDatabase>()
            private set

        var globalContext by Delegates.notNull<Context>()
            private set

        var refWatcher by Delegates.notNull<RefWatcher>()
            private set
    }

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }

        refWatcher = LeakCanary.install(this)
        globalContext = this

        val nightMode = PreferenceHelper.getNightMode(this)

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
        NotificationUtils.createNotificationChannels(this)

        messengerDatabase = Room.databaseBuilder(this, MessengerDatabase::class.java, "chat.db").build()
        tagDatabase = Room.databaseBuilder(this, TagDatabase::class.java, "tags.db").build()

        initBus()
        initApi()
        initLibs()
        enableStrictModeForDebug()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        if (BuildConfig.DEBUG) {
            MultiDex.install(this)
        }
    }

    private fun initBus() {
        bus.register(LoginEvent::class.java)
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors {
                MessengerWorker.enqueueSynchronizationIfPossible(this)
                NotificationWorker.enqueueIfPossible(this)
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

    private fun initApi() {
        api = ProxerApi.Builder(BuildConfig.PROXER_API_KEY)
            .userAgent(USER_AGENT)
            .loggingStrategy(if (BuildConfig.DEBUG) LoggingStrategy.ALL else LoggingStrategy.NONE)
            .loggingTag(LOGGING_TAG)
            .loginTokenManager(ProxerLoginTokenManager())
            .client(OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build())
            .build()
    }

    private fun initLibs() {
        CaocConfig.Builder.create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_CRASH)
            .apply()

        EmojiManager.install(IosEmojiProvider())
        AndroidThreeTen.init(this)

        ExoMedia.setDataSourceFactoryProvider { _, listener ->
            OkHttpDataSourceFactory(client, GENERIC_USER_AGENT, listener)
        }

        WorkManager.initialize(this, Configuration.Builder().build())

        RxJavaPlugins.setErrorHandler { error ->
            when (error) {
                is UndeliverableException -> Log.w(LOGGING_TAG, "Can't deliver error: $error")
                is InterruptedException -> Log.w(LOGGING_TAG, error)
                else -> Thread.currentThread().uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), error)
            }
        }

        DrawerImageLoader.init(ConcreteDrawerImageLoader())
        SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.RGB_565)
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
