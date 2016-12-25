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
import com.proxerme.app.fragment.framework.PagedLoadingFragment.PagedInput
import com.proxerme.app.manager.UserManager
import com.proxerme.app.task.framework.CachedTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.task.framework.ValidatingTask
import com.proxerme.app.util.*
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
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
        hasReachedEnd = data.size < itemsOnPage

        adapter.append(data)

        showEmptyIfAppropriate()
        onItemsInserted(data)
    }

    protected val exceptionCallback = { exception: Exception ->
        val action = ErrorUtils.handle(activity as MainActivity, exception)

        showError(action.message, action.buttonMessage, action.buttonAction)
    }

    protected val refreshSuccessCallback = { data: Array<T> ->
        adapter.insertAndScrollUpIfNecessary(list.layoutManager, list, data)

        showEmptyIfAppropriate()
        onItemsInserted(data)
    }

    protected val refreshExceptionCallback = { exception: Exception ->
        val action = ErrorUtils.handle(activity as MainActivity, exception)

        ViewUtils.makeMultilineSnackbar(root,
                getString(R.string.error_refresh, action.message),
                Snackbar.LENGTH_LONG).setAction(action.buttonMessage, action.buttonAction).show()
    }

    open protected val isSwipeToRefreshEnabled = true
    open protected val resetOnRefresh = false
    open protected val isLoginRequired = false
    open protected val isHentaiConfirmationRequired = false
    open protected val cacheStrategy = CachedTask.CacheStrategy.FULL

    open protected val isWorking: Boolean
        get() = task.isWorking || refreshTask.isWorking

    protected lateinit var task: Task<I, Array<T>>
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

        task = ValidatingTask(CachedTask(constructTask(), cacheStrategy), {
            if (isLoginRequired) {
                Validators.validateLogin()
            }
            if (isHentaiConfirmationRequired) {
                Validators.validateHentaiConfirmation(context)
            }
        }, successCallback, exceptionCallback).onStart {
            headerFooterAdapter.removeFooter()

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

    override fun onResume() {
        super.onResume()

        task.execute(constructInput(0))
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

    open protected fun showEmptyIfAppropriate() {
        if (hasReachedEnd && adapter.isEmpty()) {
            showError(getEmptyString(), null)
        }
    }

    open protected fun getEmptyString(): String {
        return getString(R.string.error_no_data)
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

    @Suppress("unused")
    @Subscribe
    fun onCaptchaSolved(@Suppress("UNUSED_PARAMETER") event: CaptchaSolvedEvent) {
        if (!(activity as MainActivity).isPaused) {
            task.reset()
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

    abstract fun constructTask(): Task<I, Array<T>>
    open protected fun constructRefreshingTask() = constructTask()

    abstract fun constructInput(page: Int): I
    open protected fun constructRefreshingInput(page: Int) = constructInput(page)

    open class PagedInput(val page: Int)
}