package me.proxer.app

import android.app.Application
import android.arch.persistence.room.Room
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v7.app.AppCompatDelegate
import android.widget.ImageView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.devbrackets.android.exomedia.ExoMedia
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
import io.reactivex.schedulers.Schedulers
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.auth.ProxerLoginTokenManager
import me.proxer.app.manga.MangaLocks
import me.proxer.app.manga.MangaNotifications
import me.proxer.app.manga.local.LocalMangaDao
import me.proxer.app.manga.local.LocalMangaDatabase
import me.proxer.app.manga.local.LocalMangaJob
import me.proxer.app.notification.AccountNotifications
import me.proxer.app.notification.NotificationJob
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.library.api.ProxerApi
import me.proxer.library.api.ProxerApi.Builder.LoggingStrategy
import okhttp3.OkHttpClient
import java.io.File
import kotlin.concurrent.write

/**
 * @author Ruben Gees
 */
class MainApplication : Application() {

    companion object {
        const val USER_AGENT = "ProxerAndroid/${BuildConfig.VERSION_NAME}"
        const val GENERIC_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"

        val bus = RxBus()

        val moshi: Moshi
            get() = api.moshi()

        val client: OkHttpClient
            get() = api.client()

        val mangaDao: LocalMangaDao
            get() = mangaDatabase.dao()

        lateinit var api: ProxerApi
            private set

        lateinit var mangaDatabase: LocalMangaDatabase
            private set

        lateinit var globalContext: MainApplication
            private set

        lateinit var refWatcher: RefWatcher
            private set
    }

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }

        AppCompatDelegate.setDefaultNightMode(PreferenceHelper.getNightMode(this))
        NotificationUtils.createNotificationChannels(this)

        mangaDatabase = Room.databaseBuilder(this, LocalMangaDatabase::class.java, "manga.db").build()

        refWatcher = LeakCanary.install(this)
        globalContext = this

        initBus()
        initApi()
        initLibs()
        enableStrictModeForDebug()
    }

    private fun initBus() {
        bus.register(LoginEvent::class.java)
                .subscribeOn(Schedulers.io())
                .subscribe {
                    NotificationJob.scheduleIfPossible(this)
                }

        bus.register(LogoutEvent::class.java)
                .subscribeOn(Schedulers.io())
                .subscribe {
                    AccountNotifications.cancel(this)
                    MangaNotifications.cancel(this)

                    LocalMangaJob.cancelAll()

                    mangaDatabase.clear()

                    MangaLocks.localLock.write {
                        File("${globalContext.filesDir}/manga").deleteRecursively()
                    }
                }
    }

    private fun initApi() {
        api = ProxerApi.Builder(BuildConfig.PROXER_API_KEY)
                .userAgent(USER_AGENT)
                .loggingStrategy(if (BuildConfig.DEBUG) LoggingStrategy.ALL else LoggingStrategy.NONE)
                .loginTokenManager(ProxerLoginTokenManager())
                .build()
    }

    private fun initLibs() {
        EmojiManager.install(IosEmojiProvider())
        AndroidThreeTen.init(this)

        ExoMedia.setDataSourceFactoryProvider { _, listener ->
            OkHttpDataSourceFactory(client, GENERIC_USER_AGENT, listener)
        }

        JobConfig.setLogcatEnabled(BuildConfig.DEBUG)
        JobManager.create(this).addJobCreator {
            when {
//                it == ChatJob.TAG -> ChatJob()
                it == NotificationJob.TAG -> NotificationJob()
                it.startsWith(LocalMangaJob.TAG) -> LocalMangaJob()
                else -> null
            }
        }

        DrawerImageLoader.init(ConcreteDrawerImageLoader())
    }

    private fun enableStrictModeForDebug() {
        if (BuildConfig.DEBUG) {
            val threadPolicy = StrictModeCompat.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()

            val vmPolicy = StrictModeCompat.VmPolicy.Builder()
                    .detectAll()
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
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(image)
        }

        override fun cancel(imageView: ImageView) = GlideApp.with(imageView).clear(imageView)

        override fun placeholder(context: Context, tag: String?): IconicsDrawable = IconicsDrawable(context)
                .icon(CommunityMaterial.Icon.cmd_account)
                .colorRes(android.R.color.white)
    }
}
