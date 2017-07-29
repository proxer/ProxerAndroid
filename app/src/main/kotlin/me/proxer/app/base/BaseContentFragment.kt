package me.proxer.app.base

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.view.clicks
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindUntilEvent
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils.ErrorAction

/**
 * @author Ruben Gees
 */
abstract class BaseContentFragment<T> : BaseFragment() {

    abstract protected val viewModel: BaseViewModel<T>

    open protected val isSwipeToRefreshEnabled = false

    open protected val root: ViewGroup by bindView(R.id.root)
    open protected val contentContainer: ViewGroup by bindView(R.id.contentContainer)
    open protected val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    open protected val errorText: TextView by bindView(R.id.errorText)
    open protected val errorButton: Button by bindView(R.id.errorButton)
    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.setColorSchemeResources(R.color.colorPrimary)
        progress.isEnabled = isSwipeToRefreshEnabled

        progress.refreshes()
                .bindToLifecycle(this)
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

        errorButton.clicks()
                .bindUntilEvent(this, Lifecycle.Event.ON_DESTROY)
                .subscribe {
                    action.buttonAction?.toClickListener(hostingActivity)?.onClick(errorButton) ?: viewModel.load()
                }
    }

    open protected fun hideError() {
        errorContainer.visibility = View.GONE
    }
}
