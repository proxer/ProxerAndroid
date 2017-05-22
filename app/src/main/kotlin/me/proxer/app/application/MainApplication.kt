package me.proxer.app.application

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v7.app.AppCompatDelegate
import android.widget.ImageView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.devbrackets.android.exomedia.ExoMedia
import com.evernote.android.job.JobManager
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.TransferListener
import com.jakewharton.threetenabp.AndroidThreeTen
import com.kirillr.strictmodehelper.StrictModeCompat
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.squareup.moshi.Moshi
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import me.proxer.app.BuildConfig
import me.proxer.app.EventBusIndex
import me.proxer.app.data.ChatDatabase
import me.proxer.app.data.LocalMangaDatabase
import me.proxer.app.event.LoginEvent
import me.proxer.app.event.LogoutEvent
import me.proxer.app.helper.NotificationHelper
import me.proxer.app.helper.PreferenceHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.app.job.ChatJob
import me.proxer.app.job.LocalMangaJob
import me.proxer.app.job.NotificationsJob
import me.proxer.app.task.manga.MangaRemovalTask
import me.proxer.app.util.Utils.GENERIC_USER_AGENT
import me.proxer.library.api.LoginTokenManager
import me.proxer.library.api.ProxerApi
import me.proxer.library.api.ProxerApi.Builder.LoggingStrategy
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.doAsync

/**
 * @author Ruben Gees
 */
class MainApplication : Application() {

    companion object {
        const val USER_AGENT = "ProxerAndroid/${BuildConfig.VERSION_NAME}"

        val moshi: Moshi
            get() = api.moshi()

        val client: OkHttpClient
            get() = api.client()

        lateinit var api: ProxerApi
            private set

        lateinit var chatDb: ChatDatabase
            private set

        lateinit var mangaDb: LocalMangaDatabase
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

        chatDb = ChatDatabase(this)
        mangaDb = LocalMangaDatabase(this)
        refWatcher = LeakCanary.install(this)
        globalContext = this

        initApi()
        initLibs()
        enableStrictModeForDebug()

        EventBus.getDefault().register(this)
    }

    @Suppress("unused")
    @Subscribe
    fun onLogin(@Suppress("UNUSED_PARAMETER") event: LoginEvent) {
        doAsync {
            ChatJob.scheduleSynchronization()
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onLogout(@Suppress("UNUSED_PARAMETER") event: LogoutEvent) {
        doAsync {
            ChatJob.cancel()
            LocalMangaJob.cancelAll()

            NotificationHelper.cancelChatNotification(this@MainApplication)

            StorageHelper.areConferencesSynchronized = false
            StorageHelper.resetChatInterval()

            chatDb.clear()
            mangaDb.clear()

            MangaRemovalTask(filesDir).execute(Unit)
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

        EventBus.builder().addIndex(EventBusIndex())
                .apply {
                    if (!BuildConfig.DEBUG) {
                        logNoSubscriberMessages(false)
                        sendNoSubscriberEvent(false)
                    }
                }
                .installDefaultEventBus()

        ExoMedia.setHttpDataSourceFactoryProvider(ExoMedia.HttpDataSourceFactoryProvider {
            _: String, listener: TransferListener<in DataSource>? ->

            OkHttpDataSourceFactory(client, GENERIC_USER_AGENT, listener)
        })

        JobManager.create(this).addJobCreator {
            when {
                it == ChatJob.TAG -> ChatJob()
                it == NotificationsJob.TAG -> NotificationsJob()
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

    private class ProxerLoginTokenManager : LoginTokenManager {
        override fun provide() = StorageHelper.user?.token
        override fun persist(loginToken: String?) {
            when (loginToken) {
                null -> {
                    if (StorageHelper.user?.token != loginToken) {
                        StorageHelper.user = null

                        EventBus.getDefault().post(LogoutEvent())
                    }
                }
                else -> {
                    // Don't do anything in case the token is not null. We save the token manually in the LoginDialog.
                }
            }
        }
    }

    private class ConcreteDrawerImageLoader : AbstractDrawerImageLoader() {
        override fun set(imageView: ImageView, uri: Uri?, placeholder: Drawable?, tag: String?) {
            GlideApp.with(imageView.context)
                    .load(uri)
                    .centerCrop()
                    .placeholder(placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imageView)
        }

        override fun cancel(imageView: ImageView) = GlideApp.with(imageView.context).clear(imageView)

        override fun placeholder(context: Context, tag: String?): Drawable? {
            return IconicsDrawable(context, CommunityMaterial.Icon.cmd_account).colorRes(android.R.color.white)
        }
    }
}
