package com.proxerme.app.application

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.orhanobut.hawk.Hawk
import com.orhanobut.hawk.Parser
import com.proxerme.app.BuildConfig
import com.proxerme.app.manager.UserManager
import com.proxerme.app.service.ChatService
import com.proxerme.library.connection.ProxerConnection
import com.squareup.moshi.Moshi
import net.danlew.android.joda.JodaTimeAndroid
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.lang.reflect.Type

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class MainApplication : Application() {

    companion object {
        lateinit var proxerConnection: ProxerConnection
            private set
    }

    override fun onCreate() {
        super.onCreate()

        val moshi = Moshi.Builder().build()

        JodaTimeAndroid.init(this)

        Hawk.init(this)
                .setParser(MoshiParser(moshi))
                .build()

        EventBus.builder()
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .installDefaultEventBus()

        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri?, placeholder: Drawable?) {
                Glide.with(imageView.context)
                        .load(uri)
                        .placeholder(placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .centerCrop()
                        .into(imageView)
            }

            override fun cancel(imageView: ImageView) {
                Glide.clear(imageView)
            }

            override fun placeholder(context: Context, tag: String): Drawable? {
                return IconicsDrawable(context,
                        CommunityMaterial.Icon.cmd_account)
                        .colorRes(android.R.color.white)
            }
        })

        proxerConnection = ProxerConnection.Builder(BuildConfig.PROXER_API_KEY, this)
                .withCustomMoshi(moshi)
                .build()

        EventBus.getDefault().register(this)
    }

    override fun onTerminate() {
        EventBus.getDefault().unregister(this)

        super.onTerminate()
    }

    @Subscribe
    fun onLoginStateChanged(@Suppress("UNUSED_PARAMETER") state: UserManager.LoginState) {
        ChatService.synchronize(this)
    }

    class MoshiParser(val moshi: Moshi) : Parser {
        override fun <T : Any?> fromJson(content: String, type: Type): T? {
            if (content.isEmpty()) {
                return null
            }

            return moshi.adapter<T>(type).fromJson(content)
        }

        override fun toJson(body: Any): String {
            return moshi.adapter(Any::class.java).toJson(body)
        }
    }
}