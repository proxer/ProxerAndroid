package me.proxer.app

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.os.Environment
import androidx.appcompat.app.AppCompatDelegate
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.mikepenz.iconics.Iconics
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.auth.LoginHandler
import me.proxer.app.util.GlideDrawerImageLoader
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.data.PreferenceHelper
import org.koin.android.ext.android.inject

/**
 * @author Ruben Gees
 */
class MainApplication : Application() {

    companion object {
        const val USER_AGENT = "ProxerAndroid/${BuildConfig.VERSION_NAME}"
        const val GENERIC_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    }

    private val loginHandler by inject<LoginHandler>()
    private val preferenceHelper by inject<PreferenceHelper>()

    override fun onCreate() {
        super.onCreate()

        FlavorInitializer.initialize(this)
        NotificationUtils.createNotificationChannels(this)

        initLibs()
        initCache()
        initNightMode()

        loginHandler.listen(this)
    }

    private fun initLibs() {
        Iconics.init(this)

        SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.RGB_565)
        DrawerImageLoader.init(GlideDrawerImageLoader())

        Completable
            .fromAction { EmojiManager.install(IosEmojiProvider()) }
            .subscribeOn(Schedulers.computation())
            .subscribe()
    }

    private fun initCache() {
        val hasExternalStorage = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

        if (!preferenceHelper.isCacheExternallySet) {
            preferenceHelper.shouldCacheExternally = hasExternalStorage
        } else if (preferenceHelper.shouldCacheExternally && !hasExternalStorage) {
            preferenceHelper.shouldCacheExternally = false
        }
    }

    @SuppressLint("CheckResult")
    private fun initNightMode() {
        AppCompatDelegate.setDefaultNightMode(preferenceHelper.themeContainer.variant.value)

        preferenceHelper.themeObservable.subscribe {
            AppCompatDelegate.setDefaultNightMode(it.variant.value)
        }
    }
}
