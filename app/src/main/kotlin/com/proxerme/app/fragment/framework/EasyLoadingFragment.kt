package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.util.ErrorHandler
import com.proxerme.library.connection.ProxerException
import org.jetbrains.anko.onClick

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class EasyLoadingFragment<T> : LoadingFragment<T>() {

    private companion object {
        private const val EXCEPTION_STATE = "fragment_easy_loading_state_exception"
    }

    protected var exception: ProxerException? = null

    open protected val progress: ProgressBar by bindView(R.id.progress)
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

    override fun onResume() {
        super.onResume()

        if (exception == null) {
            show()
        } else {
            onLoadFinishedWithError(exception!!)
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

        contentContainer.visibility = View.INVISIBLE
        errorContainer.visibility = View.INVISIBLE
        progress.visibility = View.VISIBLE
    }

    override fun onLoadFinished(result: T) {
        super.onLoadFinished(result)

        exception = null

        save(result)

        contentContainer.visibility = View.VISIBLE
        errorContainer.visibility = View.INVISIBLE
        progress.visibility = View.INVISIBLE

        show()
    }

    override fun onLoadFinishedWithError(result: ProxerException) {
        super.onLoadFinishedWithError(result)

        exception = result

        clear()
        showError(result)

        contentContainer.visibility = View.INVISIBLE
        errorContainer.visibility = View.VISIBLE
        progress.visibility = View.INVISIBLE
    }

    open protected fun showError(exception: ProxerException) {
        errorText.text = ErrorHandler.getMessageForErrorCode(context, exception)
        errorButton.text = getString(R.string.error_retry)
        errorButton.onClick {
            load()
        }
    }

    protected abstract fun save(result: T)

    protected abstract fun show()

}