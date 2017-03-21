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
import me.proxer.app.event.CaptchaSolvedEvent
import me.proxer.app.event.HentaiConfirmationEvent
import me.proxer.app.event.LoginEvent
import me.proxer.app.event.LogoutEvent
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.bindView
import me.proxer.library.api.ProxerException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * @author Ruben Gees
 */
abstract class LoadingFragment<I, O> : MainFragment() {

    open protected val isSwipeToRefreshEnabled = false
    open protected val isLoginRequired = false
    open protected val isHentaiConfirmationRequired = false

    open protected val isWorking: Boolean get() = task.isWorking

    protected lateinit var task: AndroidLifecycleTask<I, O>

    @Suppress("UNCHECKED_CAST")
    protected val state by lazy {
        val existing = childFragmentManager.findFragmentByTag("${javaClass.simpleName}state")

        if (existing is StateFragment<*>) {
            existing as StateFragment<O>
        } else {
            val new = StateFragment<O>()

            childFragmentManager.beginTransaction()
                    .add(new, "${javaClass.simpleName}state")
                    .commitNow()

            new
        }
    }

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
                .bindToLifecycle(this)
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

        EventBus.getDefault().register(this)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.setColorSchemeResources(R.color.primary, R.color.accent)

        if (isWorking) {
            hideContent()
            hideError()
        }

        setProgressVisible(isWorking)

        if (savedInstanceState != null) {
            state.data?.let {
                onSuccess(it)
            }

            state.error?.let {
                onError(it)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            task.execute(constructInput())
        }
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
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLogout(@Suppress("UNUSED_PARAMETER") event: LogoutEvent) {
        if (isLoginRequired) {
            freshLoad()
        }
    }

    /**
     * ( ͡° ͜ʖ ͡°)
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHentaiConfirmation(@Suppress("UNUSED_PARAMETER") event: HentaiConfirmationEvent) {
        if (isHentaiConfirmationRequired) {
            freshLoad()
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onCaptchaSolved(@Suppress("UNUSED_PARAMETER") event: CaptchaSolvedEvent) {
        state.error?.let {
            val error = ErrorUtils.getInnermostError(it)

            if (error is ProxerException && error.serverErrorType == ProxerException.ServerErrorType.IP_BLOCKED) {
                state.error = null
            }
        }
    }

    open protected fun validate() {
        if (isLoginRequired) {
            Validators.validateLogin()
        }

        if (isHentaiConfirmationRequired) {
            Validators.validateHentaiConfirmation(context)
        }
    }

    open protected fun onSuccess(result: O) {
        hideError()
        showContent()

        saveResultToState(result)
        removeErrorFromState(result)
    }

    open protected fun onError(error: Throwable) {
        hideContent()
        handleError(error)

        saveErrorToState(error)
        removeResultFromState(error)
    }

    open protected fun handleError(error: Throwable) {
        val action = ErrorUtils.handle(activity as MainActivity, error)

        showError(action.message, action.buttonMessage, action.buttonAction)
    }

    open protected fun showError(message: Int, buttonMessage: Int = ErrorUtils.ErrorAction.ACTION_MESSAGE_DEFAULT,
                                 onButtonClickListener: View.OnClickListener? = null) {
        errorContainer.visibility = View.VISIBLE
        errorText.text = getString(message)

        errorButton.text = when (buttonMessage) {
            ErrorUtils.ErrorAction.ACTION_MESSAGE_DEFAULT -> getString(R.string.error_action_retry)
            ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE -> null
            else -> getString(buttonMessage)
        }

        errorButton.visibility = when (buttonMessage) {
            ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE -> View.GONE
            else -> View.VISIBLE
        }

        errorButton.setOnClickListener(onButtonClickListener ?: View.OnClickListener {
            task.freshExecute(constructInput())
        })
    }

    open protected fun hideError() {
        errorContainer.visibility = View.GONE
    }

    open protected fun showContent() {
        contentContainer.visibility = View.VISIBLE
    }

    open protected fun hideContent() {
        contentContainer.visibility = View.GONE
    }

    open protected fun setProgressVisible(visible: Boolean) {
        progress.isEnabled = if (!visible) isSwipeToRefreshEnabled else true
        progress.isRefreshing = visible
    }

    open fun saveResultToState(result: O) {
        state.data = result
    }

    open fun saveErrorToState(error: Throwable) {
        state.error = error
    }

    open protected fun removeResultFromState(error: Throwable) {
        state.data = null
    }

    open protected fun removeErrorFromState(result: O) {
        state.error = null
    }

    open fun freshLoad() {
        state.clear()

        task.freshExecute(constructInput())
    }

    abstract protected fun constructTask(): Task<I, O>
    abstract protected fun constructInput(): I
}
