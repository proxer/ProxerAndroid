package me.proxer.app.auth

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding3.widget.editorActionEvents
import com.jakewharton.rxbinding3.widget.textChanges
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.toIconicsSizeDp
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.linkClicks
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.safeText
import me.proxer.app.util.extension.toast
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Ruben Gees
 */
class LoginDialog : BaseDialog() {

    companion object {
        private val websiteRegex = Regex("Proxer \\b(.+?)\\b")

        fun show(activity: AppCompatActivity) = LoginDialog().show(activity.supportFragmentManager, "login_dialog")
    }

    private val viewModel by viewModel<LoginViewModel>()

    private val username: TextInputEditText by bindView(R.id.username)
    private val password: TextInputEditText by bindView(R.id.password)
    private val secret: TextInputEditText by bindView(R.id.secret)
    private val usernameContainer: TextInputLayout by bindView(R.id.usernameContainer)
    private val passwordContainer: TextInputLayout by bindView(R.id.passwordContainer)
    private val inputContainer: ViewGroup by bindView(R.id.inputContainer)
    private val progress: ProgressBar by bindView(R.id.progress)
    private val registrationInfo: TextView by bindView(R.id.registrationInfo)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLikelyUrl(ProxerUrls.registerWeb())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .noAutoDismiss()
        .title(R.string.dialog_login_title)
        .positiveButton(R.string.dialog_login_positive) { validateAndLogin() }
        .negativeButton(R.string.cancel) { it.dismiss() }
        .customView(R.layout.dialog_login, scrollable = true)

    override fun onDialogCreated(savedInstanceState: Bundle?) {
        super.onDialogCreated(savedInstanceState)

        if (savedInstanceState == null) {
            username.requestFocus()

            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }

        setupViews()
        setupListeners()
        setupViewModel()
    }

    private fun setupViews() {
        secret.transformationMethod = null

        registrationInfo.compoundDrawablePadding = dip(12)
        registrationInfo.text = requireContext().getString(R.string.dialog_login_registration)
            .linkify(web = false, mentions = false, custom = *arrayOf(websiteRegex))

        registrationInfo.setCompoundDrawables(generateInfoDrawable(), null, null, null)
    }

    private fun setupListeners() {
        listOf(password, secret).forEach {
            it.editorActionEvents { event -> event.actionId == EditorInfo.IME_ACTION_GO }
                .filter { event -> event.actionId == EditorInfo.IME_ACTION_GO }
                .autoDisposable(dialogLifecycleOwner.scope())
                .subscribe { validateAndLogin() }
        }

        listOf(username to usernameContainer, password to passwordContainer).forEach { (input, container) ->
            input.textChanges()
                .skipInitialValue()
                .autoDisposable(dialogLifecycleOwner.scope())
                .subscribe { setError(container, null) }
        }

        registrationInfo.linkClicks()
            .map { ProxerUrls.registerWeb() }
            .autoDisposable(dialogLifecycleOwner.scope())
            .subscribe { showPage(it) }
    }

    private fun setupViewModel() {
        viewModel.success.observe(dialogLifecycleOwner, Observer {
            it?.let {
                dismiss()
            }
        })

        viewModel.error.observe(dialogLifecycleOwner, Observer {
            it?.let {
                viewModel.error.value = null

                requireContext().toast(it.message)
            }
        })

        viewModel.isLoading.observe(dialogLifecycleOwner, Observer {
            inputContainer.isGone = it == true
            progress.isVisible = it == true
        })

        viewModel.isTwoFactorAuthenticationEnabled.observe(dialogLifecycleOwner, Observer {
            secret.isVisible = it == true
            secret.imeOptions = if (it == true) EditorInfo.IME_ACTION_GO else EditorInfo.IME_ACTION_NEXT
            password.imeOptions = if (it == true) EditorInfo.IME_ACTION_NEXT else EditorInfo.IME_ACTION_GO
        })
    }

    private fun validateAndLogin() {
        val username = username.safeText.trim().toString()
        val password = password.safeText.trim().toString()
        val secretKey = secret.safeText.trim().toString()

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

    private fun generateInfoDrawable() = IconicsDrawable(requireContext())
        .icon(CommunityMaterial.Icon2.cmd_information_outline)
        .iconColor(requireContext())
        .size(20.toIconicsSizeDp())
}
