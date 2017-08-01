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
import com.rubengees.rxbus.RxBus
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.squareup.moshi.Moshi
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import me.proxer.app.auth.ProxerLoginTokenManager
import me.proxer.app.manga.local.LocalMangaDao
import me.proxer.app.manga.local.LocalMangaDatabase
import me.proxer.app.manga.local.LocalMangaJob
import me.proxer.app.util.data.NonPersistentCookieJar
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.library.api.ProxerApi
import me.proxer.library.api.ProxerApi.Builder.LoggingStrategy
import okhttp3.OkHttpClient

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

        lateinit var api: ProxerApi
            private set

        lateinit var mangaDao: LocalMangaDao
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

        mangaDao = Room.databaseBuilder(this, LocalMangaDatabase::class.java, "manga.db").build().dao()

        refWatcher = LeakCanary.install(this)
        globalContext = this

        initApi()
        initLibs()
        enableStrictModeForDebug()
    }

    private fun initApi() {
        api = ProxerApi.Builder(BuildConfig.PROXER_API_KEY)
                .userAgent(USER_AGENT)
                .client(OkHttpClient.Builder().cookieJar(NonPersistentCookieJar()).build())
                .loggingStrategy(if (BuildConfig.DEBUG) LoggingStrategy.ALL else LoggingStrategy.NONE)
                .loginTokenManager(ProxerLoginTokenManager())
                .build()
    }

    private fun initLibs() {
        EmojiManager.install(IosEmojiProvider())
        AndroidThreeTen.init(this)

        ExoMedia.setHttpDataSourceFactoryProvider({ _: String, listener: TransferListener<in DataSource>? ->
            OkHttpDataSourceFactory(client, GENERIC_USER_AGENT, listener)
        })

        JobManager.create(this)
                .apply { config.isVerbose = BuildConfig.DEBUG }
                .addJobCreator {
                    when {
//                        it == ChatJob.TAG -> ChatJob()
//                        it == NotificationsJob.TAG -> NotificationsJob()
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
