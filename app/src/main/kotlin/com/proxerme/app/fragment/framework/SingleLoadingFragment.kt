package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.manager.UserManager
import com.proxerme.app.module.LoginUtils
import com.proxerme.app.task.CachedTask
import com.proxerme.app.task.Task
import com.proxerme.app.task.ValidatingTask
import com.proxerme.app.util.ErrorHandler
import com.proxerme.app.util.KotterKnife
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.ProxerException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

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
        context?.let {
            when (exception) {
                is ProxerException -> {
                    showError(ErrorHandler.getMessageForErrorCode(context, exception))
                }
                is LoginUtils.NotLoggedInException -> {
                    showError(getString(R.string.status_not_logged_in),
                            getString(R.string.module_login_login), View.OnClickListener {
                        LoginDialog.show(activity as AppCompatActivity)
                    })
                }
                else -> showError(context.getString(R.string.error_unknown))
            }
        }

        Unit
    }

    open protected val isSwipeToRefreshEnabled = false
    open protected val isLoginRequired = false

    private lateinit var task: Task<T>

    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)
    open protected val contentContainer: ViewGroup by bindView(R.id.contentContainer)
    open protected val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    open protected val errorText: TextView by bindView(R.id.errorText)
    open protected val errorButton: Button by bindView(R.id.errorButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        task = ValidatingTask(CachedTask(constructTask()), LoginUtils.loginValidator(isLoginRequired))
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

        progress.setColorSchemeResources(R.color.primary, R.color.accent)
        errorText.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun onStart() {
        super.onStart()

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        task.execute(successCallback, exceptionCallback)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)

        super.onStop()
    }

    override fun onDestroyView() {
        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun onDestroy() {
        task.destroy()

        super.onDestroy()
    }

    open protected fun clear() {
        task.reset()
    }

    open protected fun reset() {
        clear()

        task.execute(successCallback, exceptionCallback)
    }

    open protected fun showError(message: String, buttonMessage: String? = null,
                                 onButtonClickListener: View.OnClickListener? = null) {
        errorText.text = Utils.buildClickableText(context, message,
                onWebClickListener = Link.OnClickListener { link ->
                    Utils.viewLink(context, link + "?device=mobile")
                })

        errorButton.text = when (buttonMessage) {
            null -> getString(R.string.error_retry)
            else -> buttonMessage
        }
        errorButton.setOnClickListener(when (onButtonClickListener) {
            null -> View.OnClickListener { reset() }
            else -> onButtonClickListener
        })
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoginStateChanged(@Suppress("UNUSED_PARAMETER") loginState: UserManager.LoginState) {
        if (isLoginRequired) {
            reset()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOngoingStateChanged(@Suppress("UNUSED_PARAMETER") ongoingState: UserManager.OngoingState) {
        if (isLoginRequired) {
            reset()
        }
    }


    private fun setRefreshing(enable: Boolean) {
        progress.isEnabled = if (!enable) isSwipeToRefreshEnabled else true
        progress.isRefreshing = enable
    }

    abstract fun present(data: T)
    abstract fun constructTask(): Task<T>

}