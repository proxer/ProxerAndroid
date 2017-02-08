package com.proxerme.app.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.proxerme.app.R
import com.proxerme.app.application.MainApplication
import com.proxerme.app.entitiy.LocalUser
import com.proxerme.app.event.LoginEvent
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.util.ErrorUtils
import com.proxerme.app.util.listener.OnTextListener
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.user.entitiy.User
import com.proxerme.library.connection.user.request.LoginRequest
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.longToast

/**
 * A dialog, which shows a login mask to the user. It also handles the login and shows a ProgressBar
 * to the user.

 * @author Ruben Gees
 */
class LoginDialog : DialogFragment() {

    companion object {
        fun show(activity: AppCompatActivity) {
            LoginDialog().show(activity.supportFragmentManager, "dialog_login")
        }
    }

    private val successCallback = { it: User ->
        val username = inputUsername.text.toString()
        val password = inputPassword.text.toString()
        val savePassword = remember.isChecked

        StorageHelper.user = LocalUser(username, if (savePassword) password else null, it.id,
                it.imageId, it.loginToken)

        EventBus.getDefault().post(LoginEvent())

        dismiss()
    }

    private val exceptionCallback = { exception: Exception ->
        if (exception is ProxerException) {
            context.longToast(ErrorUtils.getMessageForErrorCode(context, exception))
        } else {
            context.longToast(R.string.error_unknown)
        }

        handleVisibility()
    }

    private lateinit var task: ProxerLoadingTask<LoginInput, User>

    private lateinit var root: ViewGroup
    private lateinit var inputUsername: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var usernameContainer: TextInputLayout
    private lateinit var passwordContainer: TextInputLayout
    private lateinit var remember: CheckBox
    private lateinit var inputContainer: ViewGroup
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        task = ProxerLoadingTask({ LoginRequest(it.username, it.password) }, successCallback,
                exceptionCallback)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .autoDismiss(false)
                .title(R.string.dialog_login_title)
                .positiveText(R.string.dialog_login_positive)
                .negativeText(R.string.dialog_cancel)
                .onPositive({ _, _ ->
                    login()
                })
                .onNegative({ materialDialog, _ ->
                    materialDialog.cancel()
                })
                .customView(initViews(), true)
                .build()
    }

    override fun onResume() {
        super.onResume()

        handleVisibility()
    }

    override fun onDestroy() {
        task.destroy()

        super.onDestroy()

        MainApplication.refWatcher.watch(this)
    }

    override fun onDestroyView() {
        if (dialog != null && retainInstance) {
            dialog.setDismissMessage(null)
        }

        super.onDestroyView()
    }

    private fun initViews(): View {
        root = View.inflate(context, R.layout.dialog_login, null) as ViewGroup
        inputUsername = root.findViewById(R.id.inputUsername) as TextInputEditText
        inputPassword = root.findViewById(R.id.inputPassword) as TextInputEditText
        usernameContainer = root.findViewById(R.id.usernameContainer) as TextInputLayout
        passwordContainer = root.findViewById(R.id.passwordContainer) as TextInputLayout
        remember = root.findViewById(R.id.remember) as CheckBox
        inputContainer = root.findViewById(R.id.inputContainer) as ViewGroup
        progress = root.findViewById(R.id.progress) as ProgressBar

        StorageHelper.user?.let {
            inputUsername.setText(it.username)
            inputPassword.setText(it.password)
        }

        inputPassword.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                login()

                return@OnEditorActionListener true
            }

            false
        })

        inputUsername.addTextChangedListener(object : OnTextListener() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                resetError(usernameContainer)
            }
        })

        inputPassword.addTextChangedListener(object : OnTextListener() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                resetError(passwordContainer)
            }
        })

        return root
    }

    private fun login() {
        if (!task.isWorking) {
            val username = inputUsername.text.toString()
            val password = inputPassword.text.toString()

            if (checkInput(username, password)) {
                task.execute(LoginInput(username, password))

                handleVisibility()
            }
        }
    }

    private fun handleVisibility() {
        if (task.isWorking) {
            inputContainer.visibility = View.INVISIBLE
            progress.visibility = View.VISIBLE
        } else {
            inputContainer.visibility = View.VISIBLE
            progress.visibility = View.INVISIBLE
        }
    }

    private fun checkInput(username: String, password: String): Boolean {
        var inputCorrect = true

        if (username.isBlank()) {
            inputCorrect = false

            setError(usernameContainer, getString(R.string.dialog_login_error_username))
        }

        if (password.isBlank()) {
            inputCorrect = false

            setError(passwordContainer, getString(R.string.dialog_login_error_password))
        }

        return inputCorrect
    }

    private fun setError(container: TextInputLayout, error: String) {
        container.error = error
        container.isErrorEnabled = true
    }

    private fun resetError(container: TextInputLayout) {
        container.error = null
        container.isErrorEnabled = false
    }

    private class LoginInput(val username: String, val password: String)
}
