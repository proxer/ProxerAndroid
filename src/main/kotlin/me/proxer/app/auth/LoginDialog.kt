package me.proxer.app.auth

import android.app.Dialog
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.support.v4.content.ContextCompat
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
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.LinkBuilder
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.functions.Predicate
import kotterknife.bindView
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.dip
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.longToast
import java.util.regex.Pattern

/**
 * @author Ruben Gees
 */
class LoginDialog : BaseDialog() {

    companion object {
        private val WEBSITE_PATTERN = Pattern.compile("Proxer \\b(.+?)\\b", Pattern.DOTALL)

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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog.Builder(safeContext)
            .autoDismiss(false)
            .title(R.string.dialog_login_title)
            .positiveText(R.string.dialog_login_positive)
            .negativeText(R.string.cancel)
            .onPositive { _, _ -> validateAndLogin() }
            .onNegative { _, _ -> dismiss() }
            .customView(R.layout.dialog_login, true)
            .build()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupViews()
        setupViewModels()

        if (savedInstanceState == null) {
            username.requestFocus()

            dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    private fun setupViews() {
        listOf(password, secret).forEach {
            it.editorActionEvents(Predicate { event -> event.actionId() == EditorInfo.IME_ACTION_GO })
                    .filter { event -> event.actionId() == EditorInfo.IME_ACTION_GO }
                    .autoDispose(this)
                    .subscribe { validateAndLogin() }
        }

        listOf(username to usernameContainer, password to passwordContainer).forEach { (input, container) ->
            input.textChanges()
                    .skipInitialValue()
                    .autoDispose(this)
                    .subscribe { setError(container, null) }
        }

        secret.transformationMethod = null

        registrationInfo.text = safeContext.getString(R.string.dialog_login_registration)
        registrationInfo.compoundDrawablePadding = dip(12)

        registrationInfo.setCompoundDrawables(generateInfoDrawable(), null, null, null)

        LinkBuilder.on(registrationInfo)
                .addLink(Link(WEBSITE_PATTERN)
                        .setTextColor(ContextCompat.getColor(safeContext, R.color.link))
                        .setUnderlined(false)
                        .setOnClickListener { showPage(ProxerUrls.webBase()) })
                .build()
    }

    private fun setupViewModels() {
        viewModel.data.observe(this, Observer {
            it?.let {
                StorageHelper.user = LocalUser(it.loginToken, it.id, username.text.trim().toString(), it.image)

                bus.post(LoginEvent())

                dismiss()
            }
        })

        viewModel.error.observe(this, Observer {
            it?.let {
                viewModel.error.value = null

                safeContext.longToast(it.message)
            }
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

    private fun generateInfoDrawable() = IconicsDrawable(safeContext)
            .icon(CommunityMaterial.Icon.cmd_information_outline)
            .colorRes(R.color.icon)
            .sizeDp(20)
}
