package com.proxerme.app.fragment.framework

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.proxerme.app.R
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.event.CaptchaSolvedEvent
import com.proxerme.app.event.HentaiConfirmationEvent
import com.proxerme.app.event.LoginEvent
import com.proxerme.app.event.LogoutEvent
import com.proxerme.app.fragment.framework.PagedLoadingFragment.PagedInput
import com.proxerme.app.task.framework.CachedTask
import com.proxerme.app.task.framework.CachedTask.CacheStrategy
import com.proxerme.app.task.framework.Task
import com.proxerme.app.task.framework.ValidatingTask
import com.proxerme.app.task.framework.ZippedTask
import com.proxerme.app.util.*
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import com.proxerme.library.connection.ProxerException
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.find

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class PagedLoadingFragment<I, T> : MainFragment() where I : PagedInput {

    protected val successCallback = { data: Array<T> ->
        if (view != null) {
            hasReachedEnd = data.size < itemsOnPage

            adapter.append(data)

            showEmptyIfAppropriate()
            onItemsInserted(data)
        }
    }

    protected val exceptionCallback = { exception: Exception ->
        if (view != null) {
            handleError(exception)
        }
    }

    protected val refreshSuccessCallback = { data: Array<T> ->
        if (view != null) {
            adapter.updateAndScrollUpIfNecessary(list.layoutManager, list, when (replaceOnRefresh) {
                true -> { it: PagingAdapter<T> -> it.replace(data) }
                false -> { it: PagingAdapter<T> -> it.insert(data) }
            })

            if (replaceOnRefresh) {
                cache.mutate { data }
            }

            showEmptyIfAppropriate()
            onItemsInserted(data)
        }
    }

    protected val refreshExceptionCallback = { exception: Exception ->
        if (view != null) {
            val action = ErrorUtils.handle(activity as MainActivity, exception)

            ViewUtils.makeMultilineSnackbar(root,
                    getString(R.string.error_refresh, action.message),
                    Snackbar.LENGTH_LONG).setAction(action.buttonMessage, action.buttonAction).show()
        }
    }

    open protected val isSwipeToRefreshEnabled = false
    open protected val replaceOnRefresh = false
    open protected val isLoginRequired = false
    open protected val isHentaiConfirmationRequired = false
    open protected val cacheStrategy = CacheStrategy.FULL

    open protected val isWorking: Boolean
        get() = task.isWorking || refreshTask.isWorking

    protected lateinit var task: Task<I, Array<T>>
    protected lateinit var cache: CachedTask<I, Array<T>>
    protected lateinit var refreshTask: Task<I, Array<T>>

    open protected val root: ViewGroup by bindView(R.id.root)
    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)
    open protected val list: RecyclerView by bindView(R.id.list)

    protected lateinit var headerFooterAdapter: EasyHeaderFooterAdapter

    abstract protected val layoutManager: RecyclerView.LayoutManager
    abstract protected val adapter: PagingAdapter<T>
    abstract protected val itemsOnPage: Int

    open protected var hasReachedEnd = false

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
            hideError()

            setRefreshing(true)
        }.onFinish {
            updateRefreshing()
        }

        refreshTask = ValidatingTask(constructRefreshingTask(), {
            if (isLoginRequired) {
                Validators.validateLogin()
            }
            if (isHentaiConfirmationRequired) {
                Validators.validateHentaiConfirmation(context)
            }
        }, refreshSuccessCallback, refreshExceptionCallback).onStart {
            headerFooterAdapter.removeFooter()

            setRefreshing(true)
        }.onFinish {
            updateRefreshing()
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
                refreshTask.execute(constructInput(0))
            }
            false -> null
        })

        setupList()
        updateRefreshing()
    }

    override fun onResume() {
        super.onResume()

        task.execute(constructInput(0))
    }

    override fun onDestroyView() {
        headerFooterAdapter.removeFooter()
        progress.setOnRefreshListener(null)
        list.clearOnScrollListeners()
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

    open protected fun onItemsInserted(items: Array<T>) {

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

    open protected fun handleError(exception: Exception) {
        val action = ErrorUtils.handle(activity as MainActivity, exception)

        showError(action.message, action.buttonMessage, action.buttonAction)
    }

    open protected fun showError(message: String, buttonMessage: String? = "",
                                 onButtonClickListener: View.OnClickListener? = null) {
        val errorContainer = when {
            headerFooterAdapter.innerAdapter.itemCount <= 0 -> {
                LayoutInflater.from(context).inflate(R.layout.layout_error, root, false)
            }
            else -> LayoutInflater.from(context).inflate(R.layout.item_error, root, false)
        }

        errorContainer.find<TextView>(R.id.errorText).text = message
        errorContainer.find<Button>(R.id.errorButton).apply {
            when (buttonMessage) {
                null -> visibility = View.GONE
                else -> {
                    visibility = View.VISIBLE
                    setOnClickListener(onButtonClickListener ?: View.OnClickListener {
                        task.reset()
                        task.execute(constructInput(calculateNextPage()))
                    })

                    when {
                        buttonMessage.isBlank() -> {
                            text = context.getString(R.string.error_action_retry)
                        }
                        else -> text = buttonMessage
                    }
                }
            }
        }

        headerFooterAdapter.setFooter(errorContainer)
    }

    open protected fun hideError() {
        headerFooterAdapter.removeFooter()
    }

    open protected fun showEmptyIfAppropriate() {
        if (hasReachedEnd && adapter.isEmpty()) {
            showError(getEmptyMessage(), null)
        }
    }

    open protected fun getEmptyMessage(): String {
        return getString(R.string.error_no_data)
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

    protected fun setRefreshing(enable: Boolean) {
        if (view != null) {
            progress.isEnabled = if (!enable) isSwipeToRefreshEnabled else true
            progress.isRefreshing = enable
        }
    }

    protected fun updateRefreshing() {
        setRefreshing(isWorking)
    }

    private fun setupList() {
        headerFooterAdapter = EasyHeaderFooterAdapter(adapter)

        list.setHasFixedSize(true)
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

    abstract fun constructTask(): Task<I, Array<T>>
    open protected fun constructRefreshingTask() = constructTask()

    abstract fun constructInput(page: Int): I
    open protected fun constructRefreshingInput(page: Int) = constructInput(page)

    open class PagedInput(val page: Int)
}