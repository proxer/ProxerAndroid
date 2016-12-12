package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.klinker.android.link_builder.Link
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.dialog.HentaiConfirmationDialog
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.event.HentaiConfirmationEvent
import com.proxerme.app.fragment.framework.PagedLoadingFragment.PagedInput
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.manager.UserManager
import com.proxerme.app.service.ChatService
import com.proxerme.app.task.framework.CachedTask
import com.proxerme.app.task.framework.ListenableTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.task.framework.ValidatingTask
import com.proxerme.app.util.*
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import com.proxerme.library.connection.ProxerException
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import okhttp3.HttpUrl
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class PagedLoadingFragment<I, T> : MainFragment() where I : PagedInput {

    protected val successCallback = { data: Array<T> ->
        hasReachedEnd = data.size < itemsOnPage

        adapter.append(data)
    }

    protected val exceptionCallback = { exception: Exception ->
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

    protected val refreshSuccessCallback = { data: Array<T> ->
        adapter.insertAndScrollUpIfNecessary(list.layoutManager, list, data)
    }

    protected val refreshExceptionCallback = { exceptionResult: Exception ->
        Snackbar.make(root, getString(R.string.error_refresh), Snackbar.LENGTH_LONG).show()
    }

    open protected val isSwipeToRefreshEnabled = true
    open protected val resetOnRefresh = false
    open protected val isLoginRequired = false
    open protected val isHentaiConfirmationRequired = false
    open protected val cacheStrategy = CachedTask.CacheStrategy.FULL

    open protected val refreshLifecycle = RefreshLifecycle.START
    open protected val isWorking: Boolean
        get() = task.isWorking || refreshTask.isWorking

    protected lateinit var task: Task<I, Array<T>>
    protected lateinit var refreshTask: Task<I, Array<T>>

    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)
    open protected val list: RecyclerView by bindView(R.id.list)
    open protected val root: ViewGroup by bindView(R.id.root)

    protected lateinit var headerFooterAdapter: EasyHeaderFooterAdapter

    abstract protected val layoutManager: RecyclerView.LayoutManager
    abstract protected val adapter: PagingAdapter<T>
    abstract protected val itemsOnPage: Int

    open protected var hasReachedEnd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        EventBus.getDefault().register(this)

        task = ValidatingTask(CachedTask(internalConstructTask(), cacheStrategy), {
            Validators.validateLogin(isLoginRequired)
            Validators.validateHentaiConfirmation(context, isHentaiConfirmationRequired)
        }, successCallback, exceptionCallback)

        refreshTask = ValidatingTask(internalConstructRefreshingTask(), {
            Validators.validateLogin(isLoginRequired)
            Validators.validateHentaiConfirmation(context, isHentaiConfirmationRequired)
        }, refreshSuccessCallback, refreshExceptionCallback)

        if (refreshLifecycle == RefreshLifecycle.CREATE) {
            task.execute(constructInput(0))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paging_default, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress.setColorSchemeResources(R.color.primary, R.color.accent)
        progress.setOnRefreshListener(when (isSwipeToRefreshEnabled) {
            true -> SwipeRefreshLayout.OnRefreshListener {
                if (resetOnRefresh) {
                    reset()
                } else {
                    refreshTask.execute(constructInput(0))
                }
            }
            false -> null
        })

        setupList()
        updateRefreshing()
    }

    override fun onStart() {
        super.onStart()

        ChatService.synchronize(context)

        if (refreshLifecycle == RefreshLifecycle.START) {
            task.execute(constructInput(0))
        }
    }

    override fun onResume() {
        super.onResume()

        NotificationHelper.cancelNotification(context, NotificationHelper.CHAT_NOTIFICATION)

        if (refreshLifecycle == RefreshLifecycle.RESUME) {
            task.execute(constructInput(0))
        }
    }

    override fun onDestroyView() {
        headerFooterAdapter.removeFooter()
        list.adapter = null
        list.layoutManager = null

        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)

        adapter.removeCallback()
        task.destroy()
        refreshTask.destroy()

        super.onDestroy()
    }

    open protected fun clear() {
        task.reset()
        refreshTask.reset()
        adapter.clear()
    }

    open protected fun reset() {
        hasReachedEnd = false

        clear()
        task.execute(constructInput(calculateNextPage()))
    }

    open protected fun showError(message: String, buttonMessage: String? = null,
                                 onButtonClickListener: View.OnClickListener? = null) {
        ErrorUtils.showError(context, message, headerFooterAdapter,
                buttonMessage = buttonMessage, parent = root,
                onWebClickListener = Link.OnClickListener { link ->
                    showPage(HttpUrl.parse(link).newBuilder()
                            .addQueryParameter("device", "mobile")
                            .build())
                },
                onButtonClickListener = onButtonClickListener ?: View.OnClickListener {
                    task.reset()
                    task.execute(constructInput(calculateNextPage()))
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

    protected fun setRefreshing(enable: Boolean) {
        progress.isEnabled = if (!enable) isSwipeToRefreshEnabled else true
        progress.isRefreshing = enable
    }

    protected fun updateRefreshing() {
        setRefreshing(isWorking)
    }

    private fun setupList() {
        headerFooterAdapter = EasyHeaderFooterAdapter(adapter)

        list.layoutManager = layoutManager
        list.adapter = headerFooterAdapter
        list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (!hasReachedEnd && !headerFooterAdapter.hasFooter() && !task.isWorking) {
                    task.reset()
                    task.execute(constructInput(calculateNextPage()))
                }
            }
        })
    }

    private fun calculateNextPage(): Int {
        if (adapter.isEmpty()) {
            return 0
        } else {
            return adapter.itemCount / itemsOnPage
        }
    }

    private fun internalConstructTask(): ListenableTask<I, Array<T>> {
        return constructTask().onStart {
            headerFooterAdapter.removeFooter()

            setRefreshing(true)
        }.onFinish {
            updateRefreshing()
        }
    }

    private fun internalConstructRefreshingTask(): ListenableTask<I, Array<T>> {
        return constructRefreshingTask().onStart {
            setRefreshing(true)
        }.onFinish {
            updateRefreshing()
        }
    }

    abstract fun constructTask(): ListenableTask<I, Array<T>>
    open protected fun constructRefreshingTask(): ListenableTask<I, Array<T>> {
        return constructTask()
    }

    abstract fun constructInput(page: Int): I
    open protected fun constructRefreshingInput(page: Int): I {
        return constructInput(page)
    }

    open class PagedInput(val page: Int)
}