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
import com.proxerme.app.dialog.HentaiConfirmationDialog
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.event.HentaiConfirmationEvent
import com.proxerme.app.manager.UserManager
import com.proxerme.app.task.framework.*
import com.proxerme.app.util.*
import com.proxerme.library.connection.ProxerException
import okhttp3.HttpUrl
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
        present(data)
    }

    private val exceptionCallback = { exception: Exception ->
        context?.let {
            when (exception) {
                is ProxerException -> {
                    showError(ErrorUtils.getMessageForErrorCode(context, exception))
                }
                is Validators.NotLoggedInException -> {
                    showError(getString(R.string.status_not_logged_in),
                            getString(R.string.module_login_login), View.OnClickListener {
                        LoginDialog.show(activity as AppCompatActivity)
                    })
                }
                is Validators.HentaiConfirmationRequiredException -> {
                    showError(getString(R.string.error_hentai_confirmation_needed),
                            getString(R.string.error_confirm),
                            onButtonClickListener = View.OnClickListener {
                                HentaiConfirmationDialog.show(activity as AppCompatActivity)
                            })
                }
                else -> showError(context.getString(R.string.error_unknown))
            }
        }

        Unit
    }

    open protected val isSwipeToRefreshEnabled = false
    open protected val isLoginRequired = false
    open protected val isHentaiConfirmationRequired = false
    open protected val cacheStrategy = CachedTask.CacheStrategy.FULL

    open protected val refreshLifecycle = RefreshLifecycle.START
    open protected val isWorking: Boolean
        get() = task.isWorking

    protected lateinit var task: Task<I, T>

    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)
    open protected val contentContainer: ViewGroup by bindView(R.id.contentContainer)
    open protected val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    open protected val errorText: TextView by bindView(R.id.errorText)
    open protected val errorButton: Button by bindView(R.id.errorButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        EventBus.getDefault().register(this)

        task = ListeningTask(ValidatingTask(CachedTask(constructTask(), cacheStrategy), {
            Validators.validateLogin(isLoginRequired)
            Validators.validateHentaiConfirmation(context, isHentaiConfirmationRequired)
        }), successCallback, exceptionCallback).onStart {
            setRefreshing(true)
            contentContainer.visibility = View.GONE
            errorContainer.visibility = View.GONE
        }.onSuccess {
            contentContainer.visibility = View.VISIBLE
        }.onException {
            contentContainer.visibility = View.GONE
            errorContainer.visibility = View.VISIBLE
        }.onFinish {
            updateRefreshing()
        }

        if (refreshLifecycle == RefreshLifecycle.CREATE) {
            task.execute(constructInput())
        }
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.setColorSchemeResources(R.color.primary, R.color.accent)
        errorText.movementMethod = TouchableMovementMethod.getInstance()

        contentContainer.visibility = View.GONE
        errorContainer.visibility = View.GONE

        updateRefreshing()
    }

    override fun onStart() {
        super.onStart()

        if (refreshLifecycle == RefreshLifecycle.START) {
            task.execute(constructInput())
        }
    }

    override fun onResume() {
        super.onResume()

        if (refreshLifecycle == RefreshLifecycle.RESUME) {
            task.execute(constructInput())
        }
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

    open protected fun showError(message: String, buttonMessage: String? = null,
                                 onButtonClickListener: View.OnClickListener? = null) {
        errorText.text = Utils.buildClickableText(context, message,
                onWebClickListener = Link.OnClickListener { link ->
                    showPage(HttpUrl.parse(link).newBuilder()
                            .addQueryParameter("device", "mobile")
                            .build())
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

    protected fun updateRefreshing() {
        setRefreshing(isWorking)
    }

    protected fun setRefreshing(enable: Boolean) {
        progress.isEnabled = if (!enable) isSwipeToRefreshEnabled else true
        progress.isRefreshing = enable
    }

    abstract fun present(data: T)
    abstract fun constructTask(): ListenableTask<I, T>
    abstract fun constructInput(): I

}