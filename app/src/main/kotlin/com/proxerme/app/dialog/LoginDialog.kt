package com.proxerme.app.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.proxerme.app.R
import com.proxerme.app.manager.UserManager
import com.proxerme.app.util.ErrorHandler
import com.proxerme.library.connection.user.entitiy.User
import org.jetbrains.anko.longToast

/**
 * A dialog, which shows a login mask to the user. It also handles the login and shows a ProgressBar
 * to the user.

 * @author Ruben Gees
 */
class LoginDialog : DialogFragment() {

    companion object {
        private const val STATE_LOADING = "dialog_login_state_loading"

        fun show(activity: AppCompatActivity) {
            LoginDialog().show(activity.supportFragmentManager, "dialog_login")
        }
    }

    private var loading: Boolean = false

    private lateinit var root: ViewGroup
    private lateinit var inputUsername: TextInputEditText
    private lateinit var inputPassword: TextInputEditText
    private lateinit var usernameContainer: TextInputLayout
    private lateinit var passwordContainer: TextInputLayout
    private lateinit var remember: CheckBox
    private lateinit var inputContainer: ViewGroup
    private lateinit var progress: ProgressBar

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .autoDismiss(false)
                .title(R.string.dialog_login_title)
                .positiveText(R.string.dialog_login_positive)
                .negativeText(R.string.dialog_cancel)
                .onPositive({ materialDialog, dialogAction ->
                    login()
                })
                .onNegative({ materialDialog, dialogAction ->
                    materialDialog.cancel()
                })
                .customView(initViews(), true)
                .build()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            loading = savedInstanceState.getBoolean(STATE_LOADING)
        }

        handleVisibility()
    }

    override fun onDestroy() {
        UserManager.cancel()

        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(STATE_LOADING, loading)
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

        UserManager.user?.let {
            inputUsername.setText(it.username)
            inputPassword.setText(it.password)
        }

        inputPassword.setOnEditorActionListener(TextView.OnEditorActionListener {
            v, actionId, event ->

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
        if (!loading) {
            val username = inputUsername.text.toString()
            val password = inputPassword.text.toString()

            if (checkInput(username, password)) {
                val remember: UserManager.SaveOption =
                        if (remember.isChecked) UserManager.SaveOption.SAVE
                        else UserManager.SaveOption.DONT_SAVE
                loading = true
                handleVisibility()

                UserManager.login(User(username, password), remember, {
                    loading = false

                    dismiss()
                }, { result ->
                    loading = false

                    handleVisibility()
                    context.longToast(ErrorHandler.getMessageForErrorCode(context, result))
                })
            }
        }
    }

    private fun handleVisibility() {
        if (loading) {
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

    private abstract class OnTextListener : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

        }

        override fun afterTextChanged(s: Editable) {

        }
    }
}
