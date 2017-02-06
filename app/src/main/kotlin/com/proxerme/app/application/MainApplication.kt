package com.proxerme.app.application

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.support.v7.app.AppCompatDelegate
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GenericLoaderFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.orhanobut.hawk.Hawk
import com.proxerme.app.BuildConfig
import com.proxerme.app.EventBusIndex
import com.proxerme.app.helper.PreferenceHelper
import com.proxerme.app.manager.UserManager
import com.proxerme.app.service.ChatService
import com.proxerme.library.connection.ProxerConnection
import com.proxerme.library.connection.ProxerException.*
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import net.danlew.android.joda.JodaTimeAndroid
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.InputStream

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class MainApplication : Application() {

    companion object {

        private const val USER_AGENT = "ProxerAndroid"

        lateinit var proxerConnection: ProxerConnection
            private set

        lateinit var refWatcher: RefWatcher
            private set
    }

    private val loginErrorHandler = ProxerConnection.ErrorListener {
        UserManager.notifyLoggedOut()

        if (UserManager.user != null) {
            UserManager.reLogin()
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }

        AppCompatDelegate.setDefaultNightMode(PreferenceHelper.getNightMode(this))

        refWatcher = LeakCanary.install(this)
        proxerConnection = ProxerConnection.Builder(BuildConfig.PROXER_API_KEY, this)
                .withCustomUserAgent("$USER_AGENT/${BuildConfig.VERSION_NAME}")
                .build().apply {
            registerErrorListener(UCP_USER_NOT_LOGGED_IN, loginErrorHandler)
            registerErrorListener(INFO_USER_NOT_LOGGED_IN, loginErrorHandler)
            registerErrorListener(MESSAGES_USER_NOT_LOGGED_IN, loginErrorHandler)
            registerErrorListener(NOTIFICATIONS_USER_NOT_LOGGED_IN, loginErrorHandler)
        }

        initLibs()
        initDrawerImageLoader()
        enableStrictModeForDebug()

        EventBus.getDefault().register(this)
    }

    override fun onTerminate() {
        EventBus.getDefault().unregister(this)
        proxerConnection.unregisterAllErrorListeners()

        super.onTerminate()
    }

    @Suppress("unused")
    @Subscribe
    fun onLoginStateChanged(@Suppress("UNUSED_PARAMETER") state: UserManager.LoginState) {
        ChatService.synchronize(this)
    }

    private fun initLibs() {
        EmojiManager.install(IosEmojiProvider())
        JodaTimeAndroid.init(this)
        Hawk.init(this).build()
        Glide.get(this).register(GlideUrl::class.java, InputStream::class.java,
                object : ModelLoaderFactory<GlideUrl, InputStream> {
                    override fun build(context: Context?, factories: GenericLoaderFactory?):
                            ModelLoader<GlideUrl, InputStream> {
                        return OkHttpUrlLoader(MainApplication.proxerConnection.httpClient)
                    }

                    override fun teardown() {}
                })
        EventBus.builder().addIndex(EventBusIndex())
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .installDefaultEventBus()
    }

    private fun initDrawerImageLoader() {
        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri?, placeholder: Drawable?,
                             tag: String?) {
                Glide.with(imageView.context)
                        .load(uri)
                        .placeholder(placeholder)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(imageView)
            }

            override fun cancel(imageView: ImageView) {
                Glide.clear(imageView)
            }

            override fun placeholder(context: Context, tag: String): Drawable? {
                return IconicsDrawable(context, CommunityMaterial.Icon.cmd_account)
                        .colorRes(android.R.color.white)
            }
        })
    }

    private fun enableStrictModeForDebug() {
        if (BuildConfig.DEBUG) {
            val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder()
                    .detectCustomSlowCalls()
                    .detectNetwork()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                threadPolicyBuilder.detectResourceMismatches()
            }

            StrictMode.setThreadPolicy(threadPolicyBuilder
                    .penaltyLog()
                    .penaltyDialog()
                    .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build())
        }
    }
}