package me.proxer.app.newbase

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseFragment
import me.proxer.app.base.CaptchaSolvedEvent
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
abstract class NewBaseContentFragment<T> : BaseFragment() {

    private companion object {
        private const val IS_SOLVING_CAPTCHA_ARGUMENT = "is_solving_captcha"
    }

    protected abstract val viewModel: NewBaseViewModel<T>

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

        progress.setColorSchemeResources(R.color.colorPrimary)
        progress.isEnabled = isSwipeToRefreshEnabled

        progress.refreshes()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.invalidate() }

        viewModel.data.observe(viewLifecycleOwner, Observer {
            when (it) {
                null -> hideData()
                else -> showData(it)
            }
        })

        viewModel.networkState.observe(viewLifecycleOwner, Observer {
            when (it) {
                NetworkState.Idle -> {
                    progress.isRefreshing = false

                    hideError()
                }
                NetworkState.Loading -> {
                    progress.isRefreshing = true

                    hideError()
                }
                is NetworkState.Error -> {
                    progress.isRefreshing = false

                    showError(it.errorAction)
                }
            }
        })
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
                when (action.buttonAction == ButtonAction.CAPTCHA) {
                    true -> {
                        isSolvingCaptcha = true

                        showPage(ProxerUrls.captchaWeb(Device.MOBILE))
                    }
                    false -> action.toClickListener(hostingActivity)?.onClick(errorButton) ?: viewModel.retry()
                }
            }
    }

    protected open fun hideError() {
        errorContainer.isGone = true
    }
}
