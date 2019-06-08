package me.proxer.app

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.work.Configuration
import androidx.work.Logger
import androidx.work.WorkManager
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.security.ProviderInstaller
import com.jakewharton.threetenabp.AndroidThreeTen
import com.kirillr.strictmodehelper.StrictModeCompat
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import me.proxer.app.util.Utils
import me.proxer.app.util.logging.TimberFileTree
import me.proxer.app.util.logging.WorkManagerTimberLogger
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

/**
 * @author Ruben Gees
 */
class MainContentProvider : ContentProvider() {

    private val safeContext get() = requireNotNull(this.context)

    override fun onCreate(): Boolean {
        enableStrictModeForDebug()
        initGlobalErrorHandler()
        initSecurity()
        initLibs()

        startKoin {
            androidContext(safeContext)

            modules(koinModules)
        }

        return true
    }

    private fun initGlobalErrorHandler() {
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            Timber.e(error)

            oldHandler?.uncaughtException(thread, error)
        }
    }

    private fun initSecurity() {
        ProviderInstaller.installIfNeededAsync(safeContext, object : ProviderInstaller.ProviderInstallListener {
            override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
                GoogleApiAvailability.getInstance().apply {
                    Timber.e("Error installing security patches with error code $errorCode")

                    if (
                        isUserResolvableError(errorCode) &&
                        Utils.isPackageInstalled(
                            safeContext.packageManager,
                            GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE
                        )
                    ) {
                        showErrorNotification(safeContext, errorCode)
                    }
                }
            }

            override fun onProviderInstalled() = Unit
        })
    }

    // TODO: Remove once api becomes public.
    @SuppressLint("RestrictedApi")
    private fun initLibs() {
        WorkManager.initialize(safeContext, Configuration.Builder().build())
        AndroidThreeTen.init(safeContext)

        if (BuildConfig.LOG) {
            Timber.plant(TimberFileTree(safeContext))

            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
        }

        Logger.setLogger(WorkManagerTimberLogger())

        RxJavaPlugins.setErrorHandler { error ->
            when (error) {
                is UndeliverableException -> Timber.e(error, "Can't deliver error")
                is InterruptedException -> Timber.w(error)
                else -> Thread.currentThread().uncaughtExceptionHandler
                    ?.uncaughtException(Thread.currentThread(), error)
            }
        }

        RxAndroidPlugins.setInitMainThreadSchedulerHandler { AndroidSchedulers.from(Looper.getMainLooper(), true) }
        RxAndroidPlugins.setMainThreadSchedulerHandler { AndroidSchedulers.from(Looper.getMainLooper(), true) }
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
                .penaltyLog()
                .build()

            StrictModeCompat.setPolicies(threadPolicy, vmPolicy)
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Nothing? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
    override fun getType(uri: Uri): Nothing? = null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Nothing? = null
}
