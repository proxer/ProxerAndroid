package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.proxerme.app.R
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.event.CaptchaSolvedEvent
import com.proxerme.app.event.HentaiConfirmationEvent
import com.proxerme.app.event.LoginEvent
import com.proxerme.app.event.LogoutEvent
import com.proxerme.app.task.framework.CachedTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.task.framework.ValidatingTask
import com.proxerme.app.task.framework.ZippedTask
import com.proxerme.app.util.ErrorUtils
import com.proxerme.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import com.proxerme.app.util.KotterKnife
import com.proxerme.app.util.Validators
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
abstract class SingleLoadingFragment<I, T> : MainFragment() {

    private val successCallback = { data: T ->
        if (view != null) {
            hideError()
            present(data)
        }
    }

    private val exceptionCallback = { exception: Exception ->
        if (view != null) {
            handleError(exception)
        }
    }

    open protected val isSwipeToRefreshEnabled = false
    open protected val isLoginRequired = false
    open protected val isHentaiConfirmationRequired = false
    open protected val cacheStrategy = CachedTask.CacheStrategy.FULL

    open protected val isWorking: Boolean
        get() = task.isWorking

    protected lateinit var task: Task<I, T>
    protected lateinit var cache: CachedTask<I, T>

    open protected val root: ViewGroup by bindView(R.id.root)
    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)
    open protected val contentContainer: ViewGroup by bindView(R.id.contentContainer)
    open protected val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    open protected val errorText: TextView by bindView(R.id.errorText)
    open protected val errorButton: Button by bindView(R.id.errorButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        EventBus.getDefault().register(this)

        cache = CachedTask(constructTask(), cacheStrategy)
        task = ValidatingTask(cache, {
            if (isLoginRequired) {
                Validators.validateLogin()
            }
            if (isHentaiConfirmationRequired) {
                Validators.validateHentaiConfirmation(context)
            }
        }, successCallback, exceptionCallback).onStart {
            setRefreshing(true)
            hideError()

            contentContainer.visibility = View.GONE
        }.onSuccess {
            if (view != null) {
                contentContainer.visibility = View.VISIBLE
            }
        }.onFinish {
            updateRefreshing()
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.setColorSchemeResources(R.color.primary, R.color.accent)
        contentContainer.visibility = View.GONE
        errorContainer.visibility = View.GONE

        updateRefreshing()
    }

    override fun onResume() {
        super.onResume()

        task.execute(constructInput())
    }

    override fun onDestroyView() {
        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)

        task.destroy()

        super.onDestroy()
    }

    open protected fun clear() {
        task.reset()
    }

    open protected fun reset() {
        clear()

        task.execute(constructInput())
    }

    open protected fun handleError(exception: Exception) {
        val action = ErrorUtils.handle(activity as MainActivity, exception)

        showError(action.message, action.buttonMessage, action.buttonAction)
    }

    open protected fun showError(message: Int, buttonMessage: Int = ACTION_MESSAGE_DEFAULT,
                                 onButtonClickListener: View.OnClickListener? = null) {
        contentContainer.visibility = View.GONE
        errorContainer.visibility = View.VISIBLE
        errorText.text = getString(message)

        errorButton.text = when (buttonMessage) {
            ACTION_MESSAGE_DEFAULT -> getString(R.string.error_action_retry)
            ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE -> null
            else -> getString(buttonMessage)
        }

        errorButton.visibility = when (buttonMessage) {
            ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE -> View.GONE
            else -> View.VISIBLE
        }

        errorButton.setOnClickListener(onButtonClickListener ?: View.OnClickListener {
            task.reset()
            task.execute(constructInput())
        })
    }

    open protected fun hideError() {
        errorContainer.visibility = View.GONE
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLogin(@Suppress("UNUSED_PARAMETER") event: LoginEvent) {
        if (isLoginRequired) {
            reset()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLogout(@Suppress("UNUSED_PARAMETER") event: LogoutEvent) {
        if (isLoginRequired) {
            reset()
        }
    }

    /**
     * ( ͡° ͜ʖ ͡°)
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHentaiConfirmation(@Suppress("UNUSED_PARAMETER") event: HentaiConfirmationEvent) {
        if (isHentaiConfirmationRequired) {
            reset()
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onCaptchaSolved(@Suppress("UNUSED_PARAMETER") event: CaptchaSolvedEvent) {
        cache.cachedException?.let {
            val exception = (it as? ZippedTask.PartialException)?.original ?: it

            if (exception is ProxerException &&
                    exception.proxerErrorCode == ProxerException.IP_BLOCKED) {
                cache.clear(CachedTask.CacheStrategy.EXCEPTION)
            }
        }
    }

    protected fun updateRefreshing() {
        setRefreshing(isWorking)
    }

    protected fun setRefreshing(enable: Boolean) {
        if (view != null) {
            progress.isEnabled = if (!enable) isSwipeToRefreshEnabled else true
            progress.isRefreshing = enable
        }
    }

    abstract fun present(data: T)
    abstract fun constructTask(): Task<I, T>
    abstract fun constructInput(): I

}
