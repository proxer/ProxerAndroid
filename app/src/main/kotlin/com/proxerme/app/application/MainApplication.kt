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
import com.proxerme.app.entitiy.LocalUser
import com.proxerme.app.event.LoginEvent
import com.proxerme.app.event.LogoutEvent
import com.proxerme.app.helper.PreferenceHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.service.ChatService
import com.proxerme.library.connection.ProxerConnection
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.ProxerRequest
import com.proxerme.library.connection.user.request.LoginRequest
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.squareup.moshi.Moshi
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import net.danlew.android.joda.JodaTimeAndroid
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.InputStream
import java.util.concurrent.Future

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class MainApplication : Application() {

    companion object {
        private const val USER_AGENT = "ProxerAndroid/${BuildConfig.VERSION_NAME}"

        private lateinit var proxerConnection: ProxerConnection

        lateinit var refWatcher: RefWatcher
            private set

        val httpClient: OkHttpClient
            get() = proxerConnection.httpClient

        val moshi: Moshi
            get() = proxerConnection.moshi

        fun <T> exec(request: ProxerRequest<T>, callback: (T) -> Unit,
                     errorCallback: (ProxerException) -> Unit): ProxerTokenCall<T> {
            return ProxerTokenCall(request, callback, errorCallback)
        }

        @Throws(ProxerException::class)
        fun <T> execSync(request: ProxerRequest<T>): T {
            try {
                return proxerConnection.executeSynchronized(request, StorageHelper.user?.loginToken)
            } catch (exception: ProxerException) {
                when (exception.proxerErrorCode) {
                    ProxerException.INVALID_TOKEN,
                    ProxerException.INFO_USER_NOT_LOGGED_IN,
                    ProxerException.NOTIFICATIONS_USER_NOT_LOGGED_IN,
                    ProxerException.MESSAGES_USER_NOT_LOGGED_IN,
                    ProxerException.UCP_USER_NOT_LOGGED_IN -> {
                        val user = StorageHelper.user

                        if (user?.password != null) {
                            val result = try {
                                proxerConnection.executeSynchronized(LoginRequest(user.username,
                                        user.password))
                            } catch (exception: ProxerException) {
                                when (exception.proxerErrorCode){
                                     ProxerException.LOGIN_INVALID_CREDENTIALS,
                                     ProxerException.LOGIN_MISSING_CREDENTIALS-> {
                                         StorageHelper.user = null

                                         EventBus.getDefault().post(LogoutEvent())
                                     }
                                }

                                throw exception
                            }

                            StorageHelper.user = LocalUser(user.username, user.password,
                                    result.id, result.imageId, result.loginToken)

                            EventBus.getDefault().post(LoginEvent())

                            return proxerConnection.executeSynchronized(request, result.loginToken)
                        } else {
                            throw exception
                        }
                    }
                    else -> throw exception
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }

        AppCompatDelegate.setDefaultNightMode(PreferenceHelper.getNightMode(this))

        refWatcher = LeakCanary.install(this)
        proxerConnection = ProxerConnection.Builder(BuildConfig.PROXER_API_KEY)
                .withCustomUserAgent(USER_AGENT)
                .build()

        initLibs()
        initDrawerImageLoader()
        enableStrictModeForDebug()

        EventBus.getDefault().register(this)
    }

    override fun onTerminate() {
        EventBus.getDefault().unregister(this)

        super.onTerminate()
    }

    @Suppress("unused")
    @Subscribe
    fun onLogin(@Suppress("UNUSED_PARAMETER") event: LoginEvent) {
        ChatService.synchronize(this)
    }

    @Suppress("unused")
    @Subscribe
    fun onLogout(@Suppress("UNUSED_PARAMETER") event: LogoutEvent) {
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

    class ProxerTokenCall<T>(request: ProxerRequest<T>, callback: (T) -> Unit,
                             errorCallback: (ProxerException) -> Unit) {

        private val task: Future<Unit>

        init {
            task = doAsync {
                try {
                    val result = execSync(request)

                    uiThread {
                        callback.invoke(result)
                    }
                } catch(exception: ProxerException) {
                    uiThread {
                        errorCallback.invoke(exception)
                    }
                }
            }
        }

        fun cancel() {
            task.cancel(true)
        }
    }
}