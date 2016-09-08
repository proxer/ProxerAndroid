package com.proxerme.app.fragment.framework

import adapter.FooterAdapter
import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.SQLException
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.CallSuper
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.bindView
import com.klinker.android.link_builder.Link
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.adapter.framework.PagingAdapter.PagingAdapterCallback
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.module.LoginModule
import com.proxerme.app.service.ChatService
import com.proxerme.app.util.ErrorHandler
import com.proxerme.app.util.Utils
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.interfaces.IdItem
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.util.concurrent.Future

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
abstract class EasyChatServiceFragment<T, C : PagingAdapterCallback<T>> :
        MainFragment() where T : IdItem, T : Parcelable {

    private companion object {
        private const val EXCEPTION_STATE = "fragment_easy_chat_service_state_exception"
    }

    protected val loginModule = LoginModule(object : LoginModule.LoginModuleCallback {
        override val activity: AppCompatActivity
            get() = this@EasyChatServiceFragment.activity as AppCompatActivity

        override fun showError(message: String, buttonMessage: String?,
                               onButtonClickListener: View.OnClickListener?) {
            this@EasyChatServiceFragment.showError(message, buttonMessage, onButtonClickListener,
                    true)
        }

        override fun load(showProgress: Boolean) {
            refresh()
        }
    })

    abstract protected var layoutManager: RecyclerView.LayoutManager
    abstract protected val adapter: PagingAdapter<T, C>
    protected lateinit var footerAdapter: FooterAdapter

    open protected val root: ViewGroup by bindView(R.id.root)
    open protected val list: RecyclerView by bindView(R.id.list)
    open protected val progress: SwipeRefreshLayout by bindView(R.id.progress)

    open protected val canLoad: Boolean
        get() = exception == null && loginModule.canLoad() && refreshTask?.isDone ?: true

    abstract protected val hasReachedEnd: Boolean
    abstract protected val isLoading: Boolean

    private var exception: Exception? = null
    private var refreshTask: Future<Unit>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            exception = it.getSerializable(EXCEPTION_STATE) as Exception?
        }
    }

    override fun onResume() {
        super.onResume()

        loginModule.onResume()

        if (canLoad) {
            refresh()
        }

        if (exception != null) {
            showError(exception!!)
        }

        ChatService.synchronize(context)
        NotificationHelper.cancelNotification(context, NotificationHelper.CHAT_NOTIFICATION)
    }

    override fun onStart() {
        super.onStart()

        loginModule.onStart()
    }

    override fun onStop() {
        loginModule.onStop()

        super.onStop()
    }

    override fun onDestroy() {
        refreshTask?.cancel(true)

        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paging_default, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        footerAdapter = FooterAdapter(adapter)

        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = footerAdapter
        list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (exception == null && loginModule.canLoad() && !isLoading && !hasReachedEnd) {
                    startLoadMore()
                }
            }
        })

        progress.setColorSchemeResources(R.color.primary)
        progress.isEnabled = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(EXCEPTION_STATE, exception)
    }

    open protected fun showError(exception: Exception) {
        this.exception = exception

        if (exception is ProxerException) {
            showError(ErrorHandler.getMessageForErrorCode(context, exception))
        } else if (exception is ChatService.LoadMoreMessagesException) {
            showError(exception.message!!, onButtonClickListener = View.OnClickListener {
                hideError()

                if (loginModule.canLoad()) {
                    startLoadMore()
                }
            })
        } else if (exception is SQLException) {
            showError(context.getString(R.string.error_io), clear = true)
        }
    }

    @CallSuper
    open protected fun showError(message: String, buttonMessage: String? = null,
                                 onButtonClickListener: View.OnClickListener? = null,
                                 clear: Boolean = false) {
        hideProgress()

        val onWebClickListener = Link.OnClickListener { link ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link + "?device=mobile")))
            } catch (exception: ActivityNotFoundException) {
                context.toast(R.string.link_error_not_found)
            }
        }

        val onButtonClickListenerToSet = onButtonClickListener ?: View.OnClickListener {
            if (loginModule.canLoad()) {
                refresh()
            }
        }

        Utils.showError(context, message, footerAdapter, buttonMessage, root,
                onWebClickListener = onWebClickListener,
                onButtonClickListener = onButtonClickListenerToSet)

        if (clear) {
            clear()
        }
    }

    @CallSuper
    open protected fun refresh() {
        refreshTask?.cancel(true)

        hideError()

        refreshTask = doAsync {
            try {
                val items = loadFromDB()

                if (items.isEmpty()) {
                    if (!hasReachedEnd) {
                        uiThread {
                            showProgress()
                        }

                        if (!isLoading) {
                            startLoadMore()
                        }
                    } else {
                        uiThread {
                            hideProgress()

                            //TODO show empty view
                        }
                    }
                } else {
                    uiThread {
                        hideError()
                        hideProgress()

                        adapter.replace(items)
                    }
                }
            } catch(exception: SQLException) {
                uiThread {
                    showError(exception)
                    hideProgress()
                }
            }
        }
    }

    protected abstract fun loadFromDB(): Collection<T>

    protected abstract fun startLoadMore()

    @CallSuper
    open protected fun hideError() {
        exception = null

        footerAdapter.removeFooter()
    }

    @CallSuper
    open protected fun clear() {
        adapter.clear()
    }

    private fun showProgress() {
        progress.isEnabled = true
        progress.isRefreshing = true
    }

    private fun hideProgress() {
        progress.isEnabled = false
        progress.isRefreshing = false
    }

}