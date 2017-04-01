package me.proxer.app.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.dialog.base.MainDialog
import me.proxer.app.entity.LocalUser
import me.proxer.app.event.UserChangedEvent
import me.proxer.app.helper.StorageHelper
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.listener.TextWatcherWrapper
import me.proxer.library.entitiy.user.User
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.longToast

/**
 * @author Ruben Gees
 */
class LoginDialog : MainDialog() {

    companion object {
        fun show(activity: AppCompatActivity) {
            LoginDialog().show(activity.supportFragmentManager, "login_dialog")
        }
    }

    private val task by lazy {
        TaskBuilder.asyncProxerTask<User>()
                .bindToLifecycle(this)
                .onInnerStart {
                    setProgressVisible(true)
                }
                .onSuccess {
                    StorageHelper.user = LocalUser(it.id, username.text.trim().toString(), it.image)

                    EventBus.getDefault().post(UserChangedEvent())

                    dismiss()
                }
                .onError {
                    context.longToast(ErrorUtils.handle(activity as MainActivity, it).message)

                    setProgressVisible(false)
                }
                .onFinish {
                }
                .build()
    }

    private val username: TextInputEditText by bindView(R.id.username)
    private val password: TextInputEditText by bindView(R.id.password)
    private val usernameContainer: TextInputLayout by bindView(R.id.usernameContainer)
    private val passwordContainer: TextInputLayout by bindView(R.id.passwordContainer)
    private val inputContainer: ViewGroup by bindView(R.id.inputContainer)
    private val progress: ProgressBar by bindView(R.id.progress)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .autoDismiss(false)
                .title(R.string.dialog_login_title)
                .positiveText(R.string.dialog_login_positive)
                .negativeText(R.string.dialog_cancel)
                .onPositive({ _, _ ->
                    login()
                })
                .onNegative({ _, _ ->
                    dismiss()
                })
                .customView(R.layout.dialog_login, true)
                .build()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            username.setText(StorageHelper.user?.name)
        }

        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                login()

                return@OnEditorActionListener true
            }

            false
        })

        username.addTextChangedListener(object : TextWatcherWrapper {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                resetError(usernameContainer)
            }
        })

        password.addTextChangedListener(object : TextWatcherWrapper {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                resetError(passwordContainer)
            }
        })
    }

    override fun onResume() {
        super.onResume()

        setProgressVisible(task.isWorking)
    }

    private fun login() {
        if (!task.isWorking) {
            val username = username.text.trim().toString()
            val password = password.text.trim().toString()

            if (validateInput(username, password)) {
                // This is important to avoid sending broken login tokens and making it impossible to login again.
                StorageHelper.loginToken = null

                task.execute(api.user().login(username, password).build())
            }
        }
    }

    private fun setProgressVisible(visible: Boolean) {
        inputContainer.visibility = if (visible) View.GONE else View.VISIBLE
        progress.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isBlank()) {
            setError(usernameContainer, getString(R.string.dialog_login_error_username))

            return false
        }

        if (password.isBlank()) {
            setError(passwordContainer, getString(R.string.dialog_login_error_password))

            return false
        }

        return true
    }

    private fun setError(container: TextInputLayout, errorText: String) {
        container.isErrorEnabled = true
        container.error = errorText
    }

    private fun resetError(container: TextInputLayout) {
        container.error = null
        container.isErrorEnabled = false
    }
}
