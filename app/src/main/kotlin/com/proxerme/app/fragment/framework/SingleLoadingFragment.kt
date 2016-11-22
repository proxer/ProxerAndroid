package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.task.CachedTask
import com.proxerme.app.task.Task
import com.proxerme.app.util.ErrorHandler
import com.proxerme.app.util.KotterKnife
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.ProxerException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class SingleLoadingFragment<T> : MainFragment() {

    private val successCallback = { data: T ->
        present(data)
    }

    private val exceptionCallback = { exception: Exception ->
        val message = if (exception is ProxerException) {
            ErrorHandler.getMessageForErrorCode(context, exception)
        } else context.getString(R.string.error_unknown)

        doShowError(message)
    }

    open protected val isSwipeToRefreshEnabled = false

    private lateinit var task: Task<T>

    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)
    open protected val contentContainer: ViewGroup by bindView(R.id.contentContainer)
    open protected val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    open protected val errorText: TextView by bindView(R.id.errorText)
    open protected val errorButton: Button by bindView(R.id.errorButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        task = CachedTask(constructTask())
                .onStart {
                    setRefreshing(true)
                    contentContainer.visibility = View.GONE
                    errorContainer.visibility = View.GONE
                }
                .onSuccess {
                    contentContainer.visibility = View.VISIBLE
                }
                .onException {
                    errorContainer.visibility = View.VISIBLE
                }
                .onFinish {
                    setRefreshing(false)
                }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        KotterKnife.reset(this)

        progress.setColorSchemeResources(R.color.primary, R.color.accent)
        progress.isEnabled = isSwipeToRefreshEnabled
        errorText.movementMethod = TouchableMovementMethod.getInstance()

        task.execute(successCallback, exceptionCallback)
    }

    override fun onDestroy() {
        task.destroy()

        super.onDestroy()
    }

    open protected fun doShowError(message: String, buttonMessage: String? = null,
                                   onButtonClickListener: View.OnClickListener? = null) {
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
                reset()
            }
        } else {
            errorButton.setOnClickListener {
                onButtonClickListener.onClick(it)
            }
        }
    }

    open protected fun reset() {
        task.reset()
        task.execute(successCallback, exceptionCallback)
    }

    private fun setRefreshing(enable: Boolean) {
        if (isSwipeToRefreshEnabled) {
            progress.isEnabled = enable
        }

        progress.isRefreshing = enable
    }

    abstract fun present(data: T)
    abstract fun constructTask(): Task<T>

}