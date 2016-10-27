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

    protected open var result: T? = null
    protected open var exception: ProxerException? = null

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
            show()
        }
    }

    override fun onDestroy() {
        progress.setOnRefreshListener(null)

        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(EXCEPTION_STATE, exception)
    }

    override fun reset() {
        this.exception = null

        super.reset()
    }

    override fun clear() {
        this.result = null
    }

    override fun onLoadStarted() {
        super.onLoadStarted()

        showLoading()
    }

    override fun onLoadFinished(result: T) {
        super.onLoadFinished(result)

        this.result = result
        this.exception = null

        show()
    }

    override fun onLoadFinishedWithError(result: ProxerException) {
        super.onLoadFinishedWithError(result)

        this.result = null
        this.exception = result

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

    protected abstract fun showContent(result: T)

    private fun showLoading() {
        showProgress()

        contentContainer.visibility = View.INVISIBLE
        errorContainer.visibility = View.INVISIBLE
    }

    private fun show() {
        hideProgress()

        if (exception != null) {
            contentContainer.visibility = View.INVISIBLE
            errorContainer.visibility = View.VISIBLE

            showError()
        } else {
            contentContainer.visibility = View.VISIBLE
            errorContainer.visibility = View.INVISIBLE

            if (result != null) {
                showContent(result!!)
            } else {
                clear()
            }
        }
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