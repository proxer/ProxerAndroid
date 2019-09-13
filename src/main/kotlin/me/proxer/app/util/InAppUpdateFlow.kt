package me.proxer.app.util

import android.app.Activity
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.OnFailureListener
import com.google.android.play.core.tasks.OnSuccessListener
import me.proxer.app.R
import timber.log.Timber

/**
 * @author Ruben Gees
 */
class InAppUpdateFlow {

    companion object {
        const val REQUEST_CODE = 5276
    }

    private var appUpdateManager: AppUpdateManager? = null

    private var successListener: OnSuccessListener<AppUpdateInfo>? = null
    private var failureListener: OnFailureListener? = null
    private var progressListener: InstallStateUpdatedListener? = null

    private var snackbar: Snackbar? = null

    fun start(context: Activity, rootView: ViewGroup) {
        appUpdateManager = AppUpdateManagerFactory.create(context).also { appUpdateManager ->
            successListener = successListener(context, rootView, appUpdateManager)
            failureListener = failureListener(rootView)
            progressListener = progressListener(rootView, appUpdateManager)

            appUpdateManager.appUpdateInfo.addOnSuccessListener(successListener)
            appUpdateManager.appUpdateInfo.addOnFailureListener(failureListener)
            appUpdateManager.registerListener(progressListener)
        }
    }

    private fun successListener(
        context: Activity,
        rootView: ViewGroup,
        appUpdateManager: AppUpdateManager
    ) = OnSuccessListener<AppUpdateInfo> { appUpdateInfo ->
        if (
            appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        ) {
            snackbar = Snackbar.make(rootView, R.string.activity_update_available, Snackbar.LENGTH_INDEFINITE)
                .apply {
                    setAction(R.string.activity_update_action_download) {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            context,
                            REQUEST_CODE
                        )
                    }

                    show()
                }
        }
    }

    private fun failureListener(rootView: ViewGroup) = OnFailureListener { error ->
        Timber.e(error)

        snackbar = Snackbar.make(rootView, R.string.activity_update_error, Snackbar.LENGTH_LONG).apply { show() }
    }

    private fun progressListener(
        rootView: ViewGroup,
        appUpdateManager: AppUpdateManager
    ) = InstallStateUpdatedListener {
        if (it.installStatus() == InstallStatus.DOWNLOADED) {
            snackbar = Snackbar.make(rootView, R.string.activity_update_ready, Snackbar.LENGTH_INDEFINITE)
                .apply {
                    setAction(R.string.activity_update_action_install) {
                        appUpdateManager.completeUpdate()
                    }

                    show()
                }
        } else if (it.installStatus() == InstallStatus.CANCELED) {
            snackbar?.dismiss()
        }
    }

    fun stop() {
        appUpdateManager?.unregisterListener(progressListener)
        snackbar?.dismiss()

        appUpdateManager = null
        successListener = null
        failureListener = null
        progressListener = null
        snackbar = null
    }
}
