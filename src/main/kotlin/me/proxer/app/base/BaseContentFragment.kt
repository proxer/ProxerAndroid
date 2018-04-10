package me.proxer.app.base

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.view.clicks
import kotterknife.bindView
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.autoDispose
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
abstract class BaseContentFragment<T> : BaseFragment() {

    private companion object {
        private const val IS_SOLVING_CAPTCHA_ARGUMENT = "is_solving_chaptcha"
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

        progress.setColorSchemeResources(R.color.colorPrimary)
        progress.isEnabled = isSwipeToRefreshEnabled

        progress.refreshes()
            .autoDispose(this)
            .subscribe { viewModel.refresh() }

        viewModel.error.observe(this, Observer {
            when (it) {
                null -> hideError()
                else -> showError(it)
            }
        })

        viewModel.data.observe(this, Observer {
            when (it) {
                null -> hideData()
                else -> showData(it)
            }
        })

        viewModel.isLoading.observe(this, Observer {
            progress.isEnabled = it == true || isSwipeToRefreshEnabled
            progress.isRefreshing = it == true
        })

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
        contentContainer.visibility = View.VISIBLE
    }

    protected open fun hideData() {
        contentContainer.visibility = View.GONE
    }

    protected open fun showError(action: ErrorAction) {
        errorContainer.visibility = View.VISIBLE
        errorText.text = getString(action.message)

        errorButton.text = when (action.buttonMessage) {
            ACTION_MESSAGE_DEFAULT -> getString(R.string.error_action_retry)
            ACTION_MESSAGE_HIDE -> null
            else -> getString(action.buttonMessage)
        }

        errorButton.visibility = when (action.buttonMessage) {
            ACTION_MESSAGE_HIDE -> View.GONE
            else -> View.VISIBLE
        }

        errorButton.clicks()
            .autoDispose(this)
            .subscribe {
                when (action.message == R.string.error_captcha) {
                    true -> {
                        isSolvingCaptcha = true

                        showPage(ProxerUrls.captchaWeb(Device.MOBILE))
                    }
                    false -> action.toClickListener(hostingActivity)?.onClick(errorButton) ?: viewModel.load()
                }
            }
    }

    protected open fun hideError() {
        errorContainer.visibility = View.GONE
    }
}
