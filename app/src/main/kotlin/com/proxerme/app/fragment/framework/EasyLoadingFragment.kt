package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import butterknife.bindView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.util.ErrorHandler
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ProxerException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class EasyLoadingFragment<T> : LoadingFragment<T>() {

    private companion object {
        private const val EXCEPTION_STATE = "fragment_easy_loading_state_exception"
    }

    open protected val isSwipeToRefreshEnabled = false

    protected var exception: ProxerException? = null

    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)
    open protected val contentContainer: ViewGroup by bindView(R.id.contentContainer)
    open protected val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    open protected val errorText: TextView by bindView(R.id.errorText)
    open protected val errorButton: Button by bindView(R.id.errorButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            exception = it.getSerializable(EXCEPTION_STATE) as ProxerException?
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        errorText.movementMethod = TouchableMovementMethod.getInstance()

        initProgress()
    }

    override fun onResume() {
        super.onResume()

        if (isLoading) {
            showLoading()
        } else {
            if (exception == null) {
                showResult()
            } else {
                showError()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(EXCEPTION_STATE, exception)
    }

    override fun reset() {
        super.reset()

        exception = null
    }

    override fun onLoadStarted() {
        super.onLoadStarted()

        showLoading()
    }

    override fun onLoadFinished(result: T) {
        super.onLoadFinished(result)

        exception = null

        save(result)

        showResult()
    }

    override fun onLoadFinishedWithError(result: ProxerException) {
        super.onLoadFinishedWithError(result)

        exception = result

        clear()
        showError()
    }

    open protected fun initProgress() {
        progress.setColorSchemeResources(R.color.primary)

        progress.setOnRefreshListener {
            if (canLoad) {
                load()
            } else {
                hideProgress()
            }
        }
    }

    open protected fun doShowError(message: String, buttonMessage: String? = null,
                                   onButtonClickListener: View.OnClickListener? = null) {
        hideProgress()
        contentContainer.visibility = View.INVISIBLE
        errorContainer.visibility = View.VISIBLE

        errorText.text = Utils.buildClickableText(context, message,
                onWebClickListener = Link.OnClickListener { link ->
                    Utils.viewLink(context, link + "?device=mobile")
                })

        if (buttonMessage == null) {
            errorButton.text = getString(R.string.error_retry)
        } else {
            errorButton.text = buttonMessage
        }

        if (onButtonClickListener == null) {
            errorButton.setOnClickListener {
                load()
            }
        } else {
            errorButton.setOnClickListener {
                onButtonClickListener.onClick(it)
            }
        }
    }

    protected abstract fun save(result: T)

    protected abstract fun show()

    private fun showLoading() {
        showProgress()
        contentContainer.visibility = View.INVISIBLE
        errorContainer.visibility = View.INVISIBLE
    }

    private fun showResult() {
        hideProgress()
        contentContainer.visibility = View.VISIBLE
        errorContainer.visibility = View.INVISIBLE

        show()
    }

    private fun showError() {
        doShowError(ErrorHandler.getMessageForErrorCode(context, exception!!))
    }

    private fun showProgress() {
        progress.isEnabled = true
        progress.isRefreshing = true
    }

    private fun hideProgress() {
        progress.isEnabled = isSwipeToRefreshEnabled
        progress.isRefreshing = false
    }

}