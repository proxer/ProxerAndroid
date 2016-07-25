package com.proxerme.app.module

import android.support.annotation.StringRes
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.proxerme.app.R
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.manager.UserManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class LoginModule(private val callback: LoginModuleCallback) {

    private var internalIsLoggedIn = false

    private val isLoggedIn: Boolean
        get() = UserManager.loginState == UserManager.LoginState.LOGGED_IN

    private val isLoggingIn: Boolean
        get() = UserManager.ongoingState == UserManager.OngoingState.LOGGING_IN

    init {
        internalIsLoggedIn = isLoggedIn
    }

    fun onStart() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    fun onResume() {
        update()
    }

    fun onStop() {
        EventBus.getDefault().unregister(this)
    }

    fun canLoad(): Boolean {
        return isLoggedIn
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoginStateChanged(loginState: UserManager.LoginState) {
        update()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOngoingStateChanged(ongoingState: UserManager.OngoingState) {
        update()
    }

    private fun update() {
        if (!isLoggedIn) {
            if (isLoggingIn) {
                callback.showError(getString(R.string.status_currently_logging_in),
                        getString(R.string.dialog_cancel), View.OnClickListener {
                    UserManager.cancel()
                })
            } else {
                callback.showError(getString(R.string.status_not_logged_in),
                        getString(R.string.module_login_login), View.OnClickListener {
                    LoginDialog.show(callback.activity)
                })
            }
        } else {
            if (internalIsLoggedIn != isLoggedIn) {
                callback.load(true)
            }
        }

        internalIsLoggedIn = isLoggedIn
    }

    private fun getString(@StringRes resource: Int) = callback.activity.getString(resource)

    interface LoginModuleCallback {
        val activity: AppCompatActivity

        fun showError(message: String, buttonMessage: String?,
                      onButtonClickListener: View.OnClickListener?)

        fun load(showProgress: Boolean)
    }
}