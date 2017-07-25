package me.proxer.app.auth

import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import com.afollestad.materialdialogs.MaterialDialog
import com.github.florent37.rxsharedpreferences.RxBus
import com.jakewharton.rxbinding2.widget.RxTextView
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.R
import me.proxer.app.auth.ProxerLoginTokenManager.Companion.LOGIN_EVENT
import me.proxer.app.base.BaseDialog
import me.proxer.app.entity.LocalUser
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.bindView
import org.jetbrains.anko.longToast

/**
 * @author Ruben Gees
 */
class LoginDialog : BaseDialog() {

    companion object {
        fun show(activity: AppCompatActivity) {
            LoginDialog().show(activity.supportFragmentManager, "login_dialog")
        }
    }

    private val viewModel by lazy { ViewModelProviders.of(this).get(LoginViewModel::class.java) }

    private val username: TextInputEditText by bindView(R.id.username)
    private val password: TextInputEditText by bindView(R.id.password)
    private val secret: TextInputEditText by bindView(R.id.secret)
    private val usernameContainer: TextInputLayout by bindView(R.id.usernameContainer)
    private val passwordContainer: TextInputLayout by bindView(R.id.passwordContainer)
    private val inputContainer: ViewGroup by bindView(R.id.inputContainer)
    private val progress: ProgressBar by bindView(R.id.progress)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .autoDismiss(false)
                .title(R.string.dialog_login_title)
                .positiveText(R.string.dialog_login_positive)
                .negativeText(R.string.cancel)
                .onPositive({ _, _ -> validateAndLogin() })
                .onNegative({ _, _ -> dismiss() })
                .customView(R.layout.dialog_login, true)
                .build()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        listOf(password, secret).forEach {
            RxTextView.editorActionEvents(it, { it.actionId() == EditorInfo.IME_ACTION_GO })
                    .filter { it.actionId() == EditorInfo.IME_ACTION_GO }
                    .bindToLifecycle(this)
                    .subscribe { validateAndLogin() }
        }

        listOf(username to usernameContainer, password to passwordContainer).forEach { (input, container) ->
            RxTextView.textChanges(input)
                    .skipInitialValue()
                    .bindToLifecycle(this)
                    .subscribe { setError(container, null) }
        }

        viewModel.data.observe(this, Observer {
            it?.let {
                StorageHelper.user = LocalUser(it.loginToken, it.id, username.text.trim().toString(), it.image)
                StorageHelper.isTwoFactorAuthenticationEnabled = secret.text.isNotBlank()

                RxBus.getDefault().post(LOGIN_EVENT)

                dismiss()
            }
        })

        viewModel.error.observe(this, Observer {
            it?.let { context.longToast(it.message) }
        })

        viewModel.isLoading.observe(this, Observer {
            inputContainer.visibility = if (it == true) View.GONE else View.VISIBLE
            progress.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        viewModel.isTwoFactorAuthenticationEnabled.observe(this, Observer {
            secret.visibility = if (it == true) View.VISIBLE else View.GONE
            secret.imeOptions = if (it == true) EditorInfo.IME_ACTION_GO else EditorInfo.IME_ACTION_NEXT
            password.imeOptions = if (it == true) EditorInfo.IME_ACTION_NEXT else EditorInfo.IME_ACTION_GO
        })

        if (savedInstanceState == null) {
            username.requestFocus()

            dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    private fun validateAndLogin() {
        val username = username.text.trim().toString()
        val password = password.text.trim().toString()
        val secretKey = secret.text.trim().toString()

        if (validateInput(username, password)) {
            viewModel.login(username, password, secretKey)
        }
    }

    private fun validateInput(username: String, password: String) = when {
        username.isBlank() -> {
            setError(usernameContainer, getString(R.string.dialog_login_error_username))

            false
        }
        password.isBlank() -> {
            setError(passwordContainer, getString(R.string.dialog_login_error_password))

            false
        }
        else -> true
    }

    private fun setError(container: TextInputLayout, errorText: String?) {
        container.isErrorEnabled = errorText != null
        container.error = errorText
    }
}
