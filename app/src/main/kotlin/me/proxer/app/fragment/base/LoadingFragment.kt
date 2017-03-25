package me.proxer.app.fragment.base

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.rubengees.ktask.android.AndroidLifecycleTask
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.base.Task
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MainActivity
import me.proxer.app.event.AgeConfirmationEvent
import me.proxer.app.event.LoginEvent
import me.proxer.app.event.LogoutEvent
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.bindView
import me.proxer.library.api.ProxerException
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * @author Ruben Gees
 */
abstract class LoadingFragment<I, O> : MainFragment() {

    private companion object {
        private const val IS_SOLVING_CAPTCHA_STATE = "is_solving_captcha"
    }

    open protected val isSwipeToRefreshEnabled = false
    open protected val isLoginRequired = false
    open protected val isAgeConfirmationRequired = false

    open protected val isWorking: Boolean get() = task.isWorking

    protected lateinit var task: AndroidLifecycleTask<I, O>

    @Suppress("UNCHECKED_CAST")
    protected val state by lazy {
        val existing = childFragmentManager.findFragmentByTag("${javaClass.simpleName}state")

        if (existing is StateFragment<*>) {
            existing as StateFragment<O>
        } else {
            StateFragment<O>().apply {
                this@LoadingFragment.childFragmentManager.beginTransaction()
                        .add(this, "${this@LoadingFragment.javaClass.simpleName}state")
                        .commitNow()
            }
        }
    }

    protected var isSolvingCaptcha = false

    open protected val root: ViewGroup by bindView(R.id.root)
    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)
    open protected val contentContainer: ViewGroup by bindView(R.id.contentContainer)
    open protected val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    open protected val errorText: TextView by bindView(R.id.errorText)
    open protected val errorButton: Button by bindView(R.id.errorButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        task = TaskBuilder.task(constructTask())
                .validateBefore { validate() }
                .bindToLifecycle(this, "${javaClass.simpleName}load")
                .onInnerStart {
                    hideError()
                    hideContent()
                    setProgressVisible(true)
                }
                .onSuccess {
                    onSuccess(it)
                }
                .onError {
                    onError(it)
                }
                .onFinish {
                    setProgressVisible(isWorking)
                }
                .build()

        isSolvingCaptcha = savedInstanceState?.getBoolean(IS_SOLVING_CAPTCHA_STATE) ?: false

        EventBus.getDefault().register(this)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            state.data?.let {
                onSuccess(it)
            }

            state.error?.let {
                onError(it)
            }
        }

        progress.setColorSchemeResources(R.color.primary, R.color.accent)

        if (isWorking) {
            hideContent()
            hideError()
        }

        setProgressVisible(isWorking)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            freshLoad()
        }
    }

    override fun onResume() {
        super.onResume()

        if (isSolvingCaptcha) {
            isSolvingCaptcha = false

            freshLoad()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(IS_SOLVING_CAPTCHA_STATE, isSolvingCaptcha)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)

        super.onDestroy()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLogin(@Suppress("UNUSED_PARAMETER") event: LoginEvent) {
        if (isLoginRequired) {
            freshLoad()
        } else {
            state.error?.let {
                val error = ErrorUtils.getInnermostError(it)

                // This may happen if the login token is invalid and we require the user to login on pages, that would
                // not require a login normally.
                if (error is ProxerException && error.serverErrorType in ErrorUtils.LOGIN_ERRORS) {
                    freshLoad()
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLogout(@Suppress("UNUSED_PARAMETER") event: LogoutEvent) {
        if (isLoginRequired) {
            freshLoad()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAgeConfirmation(@Suppress("UNUSED_PARAMETER") event: AgeConfirmationEvent) {
        if (isAgeConfirmationRequired) {
            freshLoad()
        }
    }

    open protected fun validate() {
        if (isLoginRequired) {
            Validators.validateLogin()
        }

        if (isAgeConfirmationRequired) {
            Validators.validateAgeConfirmation(context)
        }
    }

    open protected fun onSuccess(result: O) {
        hideError()
        showContent()

        saveResultToState(result)
        removeErrorFromState()
    }

    open protected fun onError(error: Throwable) {
        hideContent()
        showError(handleError(error))

        saveErrorToState(error)
        removeResultFromState()
    }

    open protected fun handleError(error: Throwable): ErrorAction {
        if (error is ProxerException && error.serverErrorType == ProxerException.ServerErrorType.IP_BLOCKED) {
            return ErrorAction(R.string.error_captcha, R.string.error_action_captcha, View.OnClickListener {
                isSolvingCaptcha = true

                showPage(ProxerUrls.captchaWeb(Device.MOBILE))
            })
        } else {
            return ErrorUtils.handle(activity as MainActivity, error)
        }
    }

    open protected fun showContent() {
        contentContainer.visibility = View.VISIBLE
    }

    open protected fun hideContent() {
        contentContainer.visibility = View.GONE
    }

    protected fun showError(action: ErrorAction) {
        showError(action.message, action.buttonMessage, action.buttonAction)
    }

    open protected fun showError(message: Int, buttonMessage: Int = ErrorAction.ACTION_MESSAGE_DEFAULT,
                                 buttonAction: View.OnClickListener? = null) {
        errorContainer.visibility = View.VISIBLE
        errorText.text = getString(message)

        errorButton.text = when (buttonMessage) {
            ErrorAction.ACTION_MESSAGE_DEFAULT -> getString(R.string.error_action_retry)
            ErrorAction.ACTION_MESSAGE_HIDE -> null
            else -> getString(buttonMessage)
        }

        errorButton.visibility = when (buttonMessage) {
            ErrorAction.ACTION_MESSAGE_HIDE -> View.GONE
            else -> View.VISIBLE
        }

        errorButton.setOnClickListener(buttonAction ?: View.OnClickListener {
            freshLoad()
        })
    }

    open protected fun hideError() {
        errorContainer.visibility = View.GONE
    }

    open protected fun setProgressVisible(visible: Boolean) {
        progress.isEnabled = if (!visible) isSwipeToRefreshEnabled else true
        progress.isRefreshing = visible
    }

    open protected fun saveResultToState(result: O) {
        state.data = result
    }

    open protected fun saveErrorToState(error: Throwable) {
        state.error = error
    }

    open protected fun removeResultFromState() {
        state.data = null
    }

    open protected fun removeErrorFromState() {
        state.error = null
    }

    open protected fun freshLoad() {
        state.clear()

        task.freshExecute(constructInput())
    }

    abstract protected fun constructTask(): Task<I, O>
    abstract protected fun constructInput(): I
}
