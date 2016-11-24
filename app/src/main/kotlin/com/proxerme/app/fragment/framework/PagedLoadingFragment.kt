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
import com.proxerme.app.dialog.LoginDialog
import com.proxerme.app.manager.UserManager
import com.proxerme.app.task.CachedTask
import com.proxerme.app.task.Task
import com.proxerme.app.task.ValidatingTask
import com.proxerme.app.util.ErrorHandler
import com.proxerme.app.util.KotterKnife
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import com.proxerme.library.connection.ProxerException
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class PagedLoadingFragment<T> : MainFragment() {

    private val successCallback = { data: Array<T> ->
        hasReachedEnd = data.size < itemsOnPage

        adapter.append(data)
    }

    private val exceptionCallback = { exceptionResult: Exception ->
        context?.let {
            when (exceptionResult) {
                is ProxerException -> {
                    showError(ErrorHandler.getMessageForErrorCode(context, exceptionResult))
                }
                is NotLoggedInException -> {
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

    private val refreshSuccessCallback = { data: Array<T> ->
        adapter.insert(data)
    }

    private val refreshExceptionCallback = { exceptionResult: Exception ->
        Snackbar.make(root, getString(R.string.error_refresh), Snackbar.LENGTH_LONG).show()
    }

    private val loginValidator = {
        if (isLoginRequired) {
            if (UserManager.loginState != UserManager.LoginState.LOGGED_IN ||
                    UserManager.ongoingState != UserManager.OngoingState.NONE) {
                throw NotLoggedInException()
            }
        }
    }

    private class NotLoggedInException : Exception()

    open protected val isSwipeToRefreshEnabled = true
    open protected val isLoginRequired = false

    private lateinit var task: Task<Array<T>>
    private lateinit var refreshTask: Task<Array<T>>

    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)
    open protected val list: RecyclerView by bindView(R.id.list)
    open protected val root: ViewGroup by bindView(R.id.root)

    protected lateinit var headerFooterAdapter: EasyHeaderFooterAdapter

    abstract protected val layoutManager: RecyclerView.LayoutManager
    abstract protected val adapter: PagingAdapter<T>
    abstract protected val itemsOnPage: Int

    private var hasReachedEnd = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true

        task = ValidatingTask(CachedTask(constructTask({ calculateNextPage() })), loginValidator)
                .onStart {
                    headerFooterAdapter.removeFooter()

                    setRefreshing(true)
                }
                .onFinish {
                    updateRefreshing()
                }

        refreshTask = ValidatingTask(constructTask { 0 }, loginValidator)
                .onStart {
                    setRefreshing(true)
                }
                .onFinish {
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
                refreshTask.execute(refreshSuccessCallback, refreshExceptionCallback)
            }
            false -> null
        })

        setupList()
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
        headerFooterAdapter.removeFooter()
        adapter.removeCallback()
        list.adapter = null
        list.layoutManager = null

        KotterKnife.reset(this)

        super.onDestroyView()
    }

    override fun onDestroy() {
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
        task.execute(successCallback, exceptionCallback)
    }

    open protected fun showError(message: String, buttonMessage: String? = null,
                                 onButtonClickListener: View.OnClickListener? = null) {
        Utils.showError(context, message, headerFooterAdapter,
                buttonMessage = buttonMessage, parent = root,
                onWebClickListener = Link.OnClickListener { link ->
                    Utils.viewLink(context, link + "?device=mobile")
                },
                onButtonClickListener = onButtonClickListener ?: View.OnClickListener {
                    task.reset()
                    task.execute(successCallback, exceptionCallback)
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

    private fun setupList() {
        headerFooterAdapter = EasyHeaderFooterAdapter(adapter)

        list.layoutManager = layoutManager
        list.adapter = headerFooterAdapter
        list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (!hasReachedEnd && !headerFooterAdapter.hasFooter() && !task.isWorking) {
                    task.reset()
                    task.execute(successCallback, exceptionCallback)
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

    private fun setRefreshing(enable: Boolean) {
        progress.isEnabled = if (!enable) isSwipeToRefreshEnabled else true
        progress.isRefreshing = enable
    }

    private fun updateRefreshing() {
        setRefreshing(if (task.isWorking || refreshTask.isWorking) true else false)
    }

    abstract fun constructTask(pageCallback: () -> Int): Task<Array<T>>
}