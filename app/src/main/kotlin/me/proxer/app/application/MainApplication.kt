package me.proxer.app.application

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.support.v7.app.AppCompatDelegate
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GenericLoaderFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.devbrackets.android.exomedia.ExoMedia
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.TransferListener
import com.jakewharton.threetenabp.AndroidThreeTen
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.orhanobut.hawk.Hawk
import com.orhanobut.hawk.Parser
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.squareup.moshi.Moshi
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import me.proxer.app.BuildConfig
import me.proxer.app.EventBusIndex
import me.proxer.app.event.LoginEvent
import me.proxer.app.event.LogoutEvent
import me.proxer.app.helper.PreferenceHelper
import me.proxer.app.helper.StorageHelper
import me.proxer.library.api.LoginTokenManager
import me.proxer.library.api.ProxerApi
import okhttp3.OkHttpClient
import okio.Buffer
import org.greenrobot.eventbus.EventBus
import java.io.InputStream
import java.lang.reflect.Type

/**
 * @author Ruben Gees
 */
class MainApplication : Application() {

    companion object {
        private const val USER_AGENT = "ProxerAndroid/${BuildConfig.VERSION_NAME}"

        lateinit var moshi: Moshi
            private set

        lateinit var client: OkHttpClient
            private set

        lateinit var api: ProxerApi
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

        refWatcher = LeakCanary.install(this)

        initApi()
        initLibs()
        initDrawerImageLoader()
        enableStrictModeForDebug()
    }

    private fun initApi() {
        moshi = Moshi.Builder().build()
        client = OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                addInterceptor {
                    it.proceed(it.request().apply {
                        val bodyContent = if (body() == null) null else newBuilder().build().let {
                            Buffer().apply { it.body().writeTo(this) }.readUtf8()
                        }

                        Log.i("ProxerAndroid", "Requesting ${url()} with method ${method()}" +
                                if (bodyContent == null) "." else if (bodyContent.isBlank()) " and a blank body."
                                else " and body \"$bodyContent\".")
                    })
                }
            }
        }.build()
        api = ProxerApi.Builder(BuildConfig.PROXER_API_KEY)
                .moshi(moshi)
                .client(client)
                .userAgent(USER_AGENT)
                .loginTokenManager(object : LoginTokenManager {
                    override fun provide() = StorageHelper.loginToken
                    override fun persist(loginToken: String?) {
                        val existingLoginToken = StorageHelper.loginToken

                        StorageHelper.loginToken = loginToken

                        if (existingLoginToken != loginToken) {
                            when (loginToken) {
                                null -> EventBus.getDefault().post(LogoutEvent())
                                else -> EventBus.getDefault().post(LoginEvent())
                            }
                        }
                    }
                }).build()
    }

    private fun initLibs() {
        EmojiManager.install(IosEmojiProvider())
        AndroidThreeTen.init(this)
        Hawk.init(this).setParser(object : Parser {
            override fun <T : Any?> fromJson(content: String?, type: Type) = moshi.adapter<T>(type).fromJson(content)
            override fun toJson(body: Any) = moshi.adapter(body.javaClass).toJson(body)
        }).build()

        ExoMedia.setHttpDataSourceFactoryProvider(ExoMedia.HttpDataSourceFactoryProvider {
            userAgent: String, listener: TransferListener<in DataSource>? ->

            OkHttpDataSourceFactory(client, userAgent, listener)
        })

        Glide.get(this).register(GlideUrl::class.java, InputStream::class.java,
                object : ModelLoaderFactory<GlideUrl, InputStream> {
                    override fun build(context: Context?, factory: GenericLoaderFactory?) = OkHttpUrlLoader(client)
                    override fun teardown() {}
                })

        EventBus.builder().addIndex(EventBusIndex())
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .installDefaultEventBus()
    }

    private fun initDrawerImageLoader() {
        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri?, placeholder: Drawable?, tag: String?) {
                Glide.with(imageView.context)
                        .load(uri)
                        .placeholder(placeholder)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(imageView)
            }

            override fun cancel(imageView: ImageView) = Glide.clear(imageView)

            override fun placeholder(context: Context, tag: String?): Drawable? {
                return IconicsDrawable(context, CommunityMaterial.Icon.cmd_account).colorRes(android.R.color.white)
            }
        })
    }

    private fun enableStrictModeForDebug() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectCustomSlowCalls()
                .detectNetwork()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        detectResourceMismatches()
                    }
                }
                .penaltyLog()
                .penaltyDialog()
                .build())

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
    }
}
