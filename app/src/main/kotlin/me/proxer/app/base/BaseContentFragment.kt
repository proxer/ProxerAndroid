package me.proxer.app.base

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.jakewharton.rxbinding2.support.v4.widget.RxSwipeRefreshLayout
import com.jakewharton.rxbinding2.view.RxView
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.extension.bindView
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
abstract class BaseContentFragment<T> : BaseFragment() {

    abstract protected val viewModel: BaseViewModel<T>

    open protected val root: ViewGroup by bindView(R.id.root)
    open protected val contentContainer: ViewGroup by bindView(R.id.contentContainer)
    open protected val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    open protected val errorText: TextView by bindView(R.id.errorText)
    open protected val errorButton: Button by bindView(R.id.errorButton)
    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.setColorSchemeResources(R.color.colorPrimary)

        RxSwipeRefreshLayout.refreshes(progress)
                .bindToLifecycle(this)
                .subscribe { viewModel.refresh() }

        viewModel.data.observe(this, Observer {
            when (it) {
                null -> hideData()
                else -> showData(it)
            }
        })

        viewModel.error.observe(this, Observer {
            when (it) {
                null -> hideError()
                else -> showError(it)
            }
        })

        viewModel.isLoading.observe(this, Observer {
            progress.isRefreshing = it == true
        })

        if (savedInstanceState == null) {
            viewModel.loadIfPossible()
        }
    }

    open protected fun showData(data: T) {
        contentContainer.visibility = View.VISIBLE
    }

    open protected fun hideData() {
        contentContainer.visibility = View.GONE
    }

    open protected fun showError(action: ErrorAction) {
        errorContainer.visibility = View.VISIBLE
        errorText.text = getString(action.message)

        errorButton.text = when (action.buttonMessage) {
            ErrorAction.ACTION_MESSAGE_DEFAULT -> getString(R.string.error_action_retry)
            ErrorAction.ACTION_MESSAGE_HIDE -> null
            else -> getString(action.buttonMessage)
        }

        errorButton.visibility = when (action.buttonMessage) {
            ErrorAction.ACTION_MESSAGE_HIDE -> View.GONE
            else -> View.VISIBLE
        }

        RxView.clicks(errorButton)
                .bindToLifecycle(this)
                .subscribe {
                    when (action.buttonAction) {
                        ButtonAction.CAPTCHA -> showPage(ProxerUrls.captchaWeb(Device.MOBILE))
                        ButtonAction.LOGIN -> Unit // LoginDialog.show(this)
                        null -> {
                            viewModel.error.value = null
                            viewModel.loadIfPossible()
                        }
                    }
                }
    }

    open protected fun hideError() {
        errorContainer.visibility = View.GONE
    }
}
