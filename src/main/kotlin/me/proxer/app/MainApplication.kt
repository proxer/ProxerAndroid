package me.proxer.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.os.Looper
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.jakewharton.threetenabp.AndroidThreeTen
import com.kirillr.strictmodehelper.StrictModeCompat
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.squareup.leakcanary.LeakCanary
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import me.proxer.app.auth.LoginHandler
import me.proxer.app.util.GlideDrawerImageLoader
import me.proxer.app.util.NotificationUtils
import me.proxer.app.util.TimberFileTree
import me.proxer.app.util.data.PreferenceHelper
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.startKoin
import timber.log.Timber

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

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }

        startKoin(this, modules)

        LeakCanary.install(this)
        FlavorInitializer.initialize(this)
        NotificationUtils.createNotificationChannels(this)

        initGlobalErrorHandler()
        initSecurity()
        initLibs()
        initCache()
        initNightMode()
        enableStrictModeForDebug()

        loginHandler.listen(this)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        if (BuildConfig.DEBUG) {
            MultiDex.install(this)
        }
    }

    private fun initGlobalErrorHandler() {
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            Timber.e(error)

            oldHandler?.uncaughtException(thread, error)
        }
    }

    private fun initSecurity() {
        ProviderInstaller.installIfNeededAsync(this, object : ProviderInstaller.ProviderInstallListener {
            override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
                GoogleApiAvailability.getInstance().apply {
                    Timber.e("Error installing security patches with error code $errorCode")

                    if (isUserResolvableError(errorCode)) {
                        showErrorNotification(this@MainApplication, errorCode)
                    }
                }
            }

            override fun onProviderInstalled() = Unit
        })
    }

    private fun initLibs() {
        EmojiManager.install(IosEmojiProvider())
        AndroidThreeTen.init(this)

        if (BuildConfig.LOG) {
            Timber.plant(TimberFileTree(this))

            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
        }

        RxJavaPlugins.setErrorHandler { error ->
            when (error) {
                is UndeliverableException -> Timber.w("Can't deliver error: $error")
                is InterruptedException -> Timber.w(error)
                else -> Thread.currentThread().uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), error)
            }
        }

        RxAndroidPlugins.setInitMainThreadSchedulerHandler { AndroidSchedulers.from(Looper.getMainLooper(), true) }
        RxAndroidPlugins.setMainThreadSchedulerHandler { AndroidSchedulers.from(Looper.getMainLooper(), true) }

        DrawerImageLoader.init(GlideDrawerImageLoader())
        SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.RGB_565)
    }

    private fun initCache() {
        val hasExternalStorage = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

        if (!preferenceHelper.isCacheExternallySet) {
            preferenceHelper.shouldCacheExternally = hasExternalStorage
        } else if (preferenceHelper.shouldCacheExternally && !hasExternalStorage) {
            preferenceHelper.shouldCacheExternally = false
        }
    }

    private fun initNightMode() {
        val nightMode = preferenceHelper.nightMode

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
    }

    private fun enableStrictModeForDebug() {
        if (BuildConfig.DEBUG) {
            val threadPolicy = StrictModeCompat.ThreadPolicy.Builder()
                .detectAll()
                .permitCustomSlowCalls()
                .permitDiskWrites()
                .permitDiskReads()
                .penaltyDialog()
                .penaltyLog()
                .build()

            val vmPolicy = StrictModeCompat.VmPolicy.Builder()
                .detectAll()
                .penaltyDeath()
                .penaltyLog()
                .build()

            StrictModeCompat.setPolicies(threadPolicy, vmPolicy)
        }
    }
}
