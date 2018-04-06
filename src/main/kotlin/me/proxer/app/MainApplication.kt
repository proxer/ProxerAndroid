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
import android.widget.ImageView
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.devbrackets.android.exomedia.ExoMedia
import com.evernote.android.job.JobApi
import com.evernote.android.job.JobConfig
import com.evernote.android.job.JobManager
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
import io.reactivex.Completable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.auth.ProxerLoginTokenManager
import me.proxer.app.chat.sync.ChatDao
import me.proxer.app.chat.sync.ChatDatabase
import me.proxer.app.chat.sync.ChatJob
import me.proxer.app.chat.sync.ChatNotifications
import me.proxer.app.media.TagDao
import me.proxer.app.media.TagDatabase
import me.proxer.app.notification.AccountNotifications
import me.proxer.app.notification.NotificationJob
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.api.ProxerApi
import me.proxer.library.api.ProxerApi.Builder.LoggingStrategy
import me.proxer.library.util.ProxerUrls
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.io.File
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

        val CERTIFICATES = arrayOf(
            "sha256/58qRu/uxh4gFezqAcERupSkRYBlBAvfcw7mEjGPLnNU=",
            "sha256/grX4Ta9HpZx6tSHkmCrvpApTQGo67CYDnvprLg5yRME="
        )

        val bus = RxBus()

        val moshi: Moshi
            get() = api.moshi()

        val client: OkHttpClient
            get() = api.client()

        val chatDao: ChatDao
            get() = chatDatabase.dao()

        val tagDao: TagDao
            get() = tagDatabase.dao()

        var api by Delegates.notNull<ProxerApi>()
            private set

        var chatDatabase by Delegates.notNull<ChatDatabase>()
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

        AppCompatDelegate.setDefaultNightMode(PreferenceHelper.getNightMode(this))
        NotificationUtils.createNotificationChannels(this)

        chatDatabase = Room.databaseBuilder(this, ChatDatabase::class.java, "chat.db").build()
        tagDatabase = Room.databaseBuilder(this, TagDatabase::class.java, "tags.db").build()

        initBus()
        initApi()
        initLibs()
        enableStrictModeForDebug()

        // TODO: Remove in next update
        Completable
            .fromCallable { File("$filesDir/manga").deleteRecursively() }
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors()
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
                ChatJob.scheduleSynchronizationIfPossible(this)
                NotificationJob.scheduleIfPossible(this)
            }

        bus.register(LogoutEvent::class.java)
            .subscribeOn(Schedulers.io())
            .subscribeAndLogErrors {
                AccountNotifications.cancel(this)
                ChatNotifications.cancel(this)

                ChatJob.cancel()

                StorageHelper.lastChatMessageDate = Date(0L)
                StorageHelper.lastNotificationsDate = Date(0L)
                StorageHelper.areConferencesSynchronized = false
                StorageHelper.resetChatInterval()

                chatDao.clear()
            }
    }

    private fun initApi() {
        val certificatePinner = CertificatePinner.Builder()
            .apply {
                CERTIFICATES.forEach { add(ProxerUrls.webBase().host(), it) }
            }
            .build()

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
                .certificatePinner(certificatePinner)
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

        JobConfig.setLogcatEnabled(BuildConfig.DEBUG)
        JobConfig.setApiEnabled(JobApi.GCM, false)
        JobManager.create(this).addJobCreator {
            when (it) {
                ChatJob.TAG -> ChatJob()
                NotificationJob.TAG -> NotificationJob()
                else -> null
            }
        }

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
