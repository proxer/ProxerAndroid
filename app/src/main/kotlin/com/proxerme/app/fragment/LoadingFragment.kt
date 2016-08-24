package com.proxerme.app.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
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
import org.jetbrains.anko.longToast

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
abstract class LoadingFragment : MainFragment() {

    private companion object {
        const val STATE_LOADING = "fragment_loading_state_loading"
        const val STATE_FIRST_LOAD = "fragment_loading_state_first_load"
        const val STATE_CURRENT_ERROR = "fragment_loading_current_error"
    }

    protected var isLoading = false
    protected var ongoingLoads = 0
    protected var isFirstLoad = true
    protected var currentError: String? = null

    open protected val canLoad = true
    open protected val loadAlways = false
    open protected val parallelLoads = 1

    protected val refreshLayout: SwipeRefreshLayout by bindView(R.id.refreshLayout)
    abstract protected val errorContainer: ViewGroup
    abstract protected val errorText: TextView
    abstract protected val errorButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            isLoading = it.getBoolean(STATE_LOADING)
            isFirstLoad = it.getBoolean(STATE_FIRST_LOAD)
            currentError = it.getString(STATE_CURRENT_ERROR)
        }
    }

    override fun onResume() {
        super.onResume()

        if (currentError != null) {
            currentError?.run { showError(this) }
        } else if ((isLoading || isFirstLoad) && canLoad) {
            load(true)
        } else if (loadAlways && canLoad) {
            load(false)
        }
    }

    override fun onDestroy() {
        cancel()

        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_loading, container, false).apply {
            (this.findViewById(R.id.refreshLayout) as ViewGroup)
                    .addView(inflateView(inflater, container, savedInstanceState))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshLayout.setColorSchemeResources(R.color.primary, R.color.accent)
        refreshLayout.setOnRefreshListener {
            if (!isLoading && canLoad) {
                load(true)
            } else {
                refreshLayout.isRefreshing = false
            }
        }

        errorText.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(STATE_LOADING, isLoading)
        outState.putBoolean(STATE_FIRST_LOAD, isFirstLoad)
        outState.putString(STATE_CURRENT_ERROR, currentError)
    }

    open protected fun load(showProgress: Boolean) {
        notifyLoadStarted(showProgress)
    }

    open protected fun notifyLoadStarted(showProgress: Boolean) {
        ongoingLoads = parallelLoads
        isLoading = true
        currentError = null
        if (showProgress) {
            refreshLayout.isRefreshing = true
        }

        hideError()
    }

    open protected fun notifyLoadFinishedSuccessful(result: Any) {
        ongoingLoads--

        if (ongoingLoads <= 0) {
            isLoading = false
            isFirstLoad = false
            refreshLayout.isRefreshing = false
        }
    }

    open protected fun notifyLoadFinishedWithError(result: ProxerException) {
        cancel()

        ongoingLoads = 0
        isLoading = false
        refreshLayout.isRefreshing = false
        currentError = ErrorHandler.getMessageForErrorCode(context, result)

        showError(currentError!!)
    }

    abstract fun inflateView(inflater: LayoutInflater, container: ViewGroup?,
                             savedInstanceState: Bundle?): View

    abstract fun cancel()

    open protected fun showError(message: String, buttonMessage: String? = null,
                                 onButtonClickListener: View.OnClickListener? = null) {
        errorContainer.visibility = View.VISIBLE
        errorText.text = Utils.buildClickableText(context, message, Link.OnClickListener { link ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link + "?device=mobile")))
            } catch (exception: ActivityNotFoundException) {
                context.longToast(R.string.link_error_not_found)
            }
        })

        if (buttonMessage == null) {
            errorButton.text = getString(R.string.error_retry)
        } else {
            errorButton.text = buttonMessage
        }

        if (onButtonClickListener == null) {
            errorButton.setOnClickListener { load(true) }
        } else {
            errorButton.setOnClickListener(onButtonClickListener)
        }
    }

    open protected fun hideError() {
        errorContainer.visibility = View.INVISIBLE
    }

    open protected fun reset() {
        cancel()

        refreshLayout.isRefreshing = false
        isLoading = false
        ongoingLoads = 0
        isFirstLoad = true
        currentError = null

        if (isFirstLoad && canLoad) {
            load(true)
        }
    }
}