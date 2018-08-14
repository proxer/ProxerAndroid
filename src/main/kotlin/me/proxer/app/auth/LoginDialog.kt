package me.proxer.app.auth

import android.app.Dialog
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.jakewharton.rxbinding2.widget.editorActionEvents
import com.jakewharton.rxbinding2.widget.textChanges
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.functions.Predicate
import kotterknife.bindView
import linkClicks
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.safeText
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils
import org.jetbrains.anko.longToast

/**
 * @author Ruben Gees
 */
class LoginDialog : BaseDialog() {

    companion object {
        private val WEBSITE_REGEX = Regex("Proxer \\b(.+?)\\b")
        private const val DEVICE_PARAMETER = "device"

        fun show(activity: AppCompatActivity) = LoginDialog().show(activity.supportFragmentManager, "login_dialog")
    }

    private val viewModel by unsafeLazy { LoginViewModelProvider.get(this) }

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

        setLikelyUrl(ProxerUrls.webBase())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog.Builder(requireContext())
        .autoDismiss(false)
        .title(R.string.dialog_login_title)
        .positiveText(R.string.dialog_login_positive)
        .negativeText(R.string.cancel)
        .onPositive { _, _ -> validateAndLogin() }
        .onNegative { _, _ -> dismiss() }
        .customView(R.layout.dialog_login, true)
        .build()

    override fun onDialogCreated(savedInstanceState: Bundle?) {
        super.onDialogCreated(savedInstanceState)

        if (savedInstanceState == null) {
            username.requestFocus()

            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }

        setupViews()
        setupListeners()
        setupViewModel()
    }

    private fun setupViews() {
        secret.transformationMethod = null

        registrationInfo.compoundDrawablePadding = dip(12)
        registrationInfo.text = requireContext().getString(R.string.dialog_login_registration)
            .linkify(web = false, mentions = false, custom = *arrayOf(WEBSITE_REGEX))

        registrationInfo.setCompoundDrawables(generateInfoDrawable(), null, null, null)
    }

    private fun setupListeners() {
        listOf(password, secret).forEach {
            it.editorActionEvents(Predicate { event -> event.actionId() == EditorInfo.IME_ACTION_GO })
                .filter { event -> event.actionId() == EditorInfo.IME_ACTION_GO }
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
            .map {
                ProxerUrls.webBase()
                    .newBuilder()
                    .addPathSegment("register")
                    .setQueryParameter(DEVICE_PARAMETER, ProxerUtils.getApiEnumName(Device.DEFAULT))
                    .build()
            }
            .autoDisposable(dialogLifecycleOwner.scope())
            .subscribe { showPage(it) }
    }

    private fun setupViewModel() {
        viewModel.data.observe(dialogLifecycleOwner, Observer {
            it?.let {
                StorageHelper.user = LocalUser(it.loginToken, it.id, username.safeText.trim().toString(), it.image)

                bus.post(LoginEvent())

                dismiss()
            }
        })

        viewModel.error.observe(dialogLifecycleOwner, Observer {
            it?.let {
                viewModel.error.value = null

                requireContext().longToast(it.message)
            }
        })

        viewModel.isLoading.observe(dialogLifecycleOwner, Observer {
            inputContainer.visibility = if (it == true) View.GONE else View.VISIBLE
            progress.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        viewModel.isTwoFactorAuthenticationEnabled.observe(dialogLifecycleOwner, Observer {
            secret.visibility = if (it == true) View.VISIBLE else View.GONE
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
        .icon(CommunityMaterial.Icon.cmd_information_outline)
        .iconColor(requireContext())
        .sizeDp(20)
}
