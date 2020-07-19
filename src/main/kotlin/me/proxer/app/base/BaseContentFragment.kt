package me.proxer.app.base

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.jakewharton.rxbinding3.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.resolveColor
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
abstract class BaseContentFragment<T>(@LayoutRes contentLayoutId: Int) : BaseFragment(contentLayoutId) {

    private companion object {
        private const val IS_SOLVING_CAPTCHA_ARGUMENT = "is_solving_captcha"
    }

    protected abstract val viewModel: BaseViewModel<T>

    protected open val isSwipeToRefreshEnabled = false

    protected open val root: ViewGroup by bindView(R.id.root)
    protected open val contentContainer: ViewGroup by bindView(R.id.contentContainer)
    protected open val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    protected open val errorInnerContainer: ViewGroup by bindView(R.id.errorInnerContainer)
    protected open val errorText: TextView by bindView(R.id.errorText)
    protected open val errorButton: Button by bindView(R.id.errorButton)
    protected open val progress: SwipeRefreshLayout by bindView(R.id.progress)

    private var isSolvingCaptcha: Boolean
        get() = requireArguments().getBoolean(IS_SOLVING_CAPTCHA_ARGUMENT, false)
        set(value) = requireArguments().putBoolean(IS_SOLVING_CAPTCHA_ARGUMENT, value)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val schemeColors = requireContext().let { context ->
            intArrayOf(context.resolveColor(R.attr.colorPrimary), context.resolveColor(R.attr.colorSecondary))
        }

        progress.setColorSchemeColors(*schemeColors)
        progress.isEnabled = isSwipeToRefreshEnabled

        progress.refreshes()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.refresh() }

        viewModel.error.observe(
            viewLifecycleOwner,
            Observer {
                when (it) {
                    null -> hideError()
                    else -> showError(it)
                }
            }
        )

        viewModel.data.observe(
            viewLifecycleOwner,
            Observer {
                when (it) {
                    null -> hideData()
                    else -> showData(it)
                }
            }
        )

        viewModel.isLoading.observe(
            viewLifecycleOwner,
            Observer {
                progress.isEnabled = it == true || isSwipeToRefreshEnabled
                progress.isRefreshing = it == true
            }
        )

        if (viewModel.isLoading.value != true && viewModel.data.value == null && viewModel.error.value == null) {
            viewModel.load()
        }
    }

    override fun onResume() {
        super.onResume()

        if (isSolvingCaptcha) {
            isSolvingCaptcha = false

            bus.post(CaptchaSolvedEvent())
        }
    }

    protected open fun showData(data: T) {
        contentContainer.isVisible = true
    }

    protected open fun hideData() {
        contentContainer.isVisible = false
    }

    protected open fun showError(action: ErrorAction) {
        errorContainer.isVisible = true
        errorText.text = getString(action.message)
        errorButton.isVisible = action.buttonMessage != ACTION_MESSAGE_HIDE

        errorButton.text = when (action.buttonMessage) {
            ACTION_MESSAGE_DEFAULT -> getString(R.string.error_action_retry)
            ACTION_MESSAGE_HIDE -> null
            else -> getString(action.buttonMessage)
        }

        errorButton.clicks()
            .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
            .subscribe {
                when (action.buttonAction) {
                    ErrorAction.ButtonAction.CAPTCHA -> {
                        isSolvingCaptcha = true

                        showPage(ProxerUrls.captchaWeb(Utils.getIpAddress(), Device.MOBILE), skipCheck = true)
                    }
                    else -> action.toClickListener(hostingActivity)?.onClick(errorButton) ?: viewModel.load()
                }
            }
    }

    protected open fun hideError() {
        errorContainer.isVisible = false
    }
}
