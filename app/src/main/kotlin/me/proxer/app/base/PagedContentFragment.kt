package me.proxer.app.base

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.proxer.app.R
import me.proxer.app.auth.LoginDialog
import me.proxer.app.base.BaseAdapter.ContainerPositionResolver
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.endScrolls
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import org.jetbrains.anko.find
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
abstract class PagedContentFragment<T> : BaseContentFragment<List<T>>() {

    override abstract val viewModel: PagedViewModel<T>

    private lateinit var adapter: EasyHeaderFooterAdapter
    abstract protected val layoutManager: RecyclerView.LayoutManager
    abstract protected val innerAdapter: BaseAdapter<T, *>

    open protected val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override val errorContainer: ViewGroup
        get() = adapter.footer as ViewGroup

    override val errorText: TextView
        get() = errorContainer.find(R.id.errorText)

    override val errorButton: Button
        get() = errorContainer.find(R.id.errorButton)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paged, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        adapter = EasyHeaderFooterAdapter(innerAdapter)
        innerAdapter.positionResolver = ContainerPositionResolver(adapter)

        viewModel.refreshError.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, it.message, Snackbar.LENGTH_LONG, it.buttonMessage, when (it.buttonAction) {
                    ButtonAction.CAPTCHA -> View.OnClickListener { showPage(ProxerUrls.captchaWeb(Device.MOBILE)) }
                    ButtonAction.LOGIN -> View.OnClickListener { LoginDialog.show(activity as AppCompatActivity) }
                    null -> View.OnClickListener { viewModel.refresh() }
                })

                viewModel.refreshError.value = null
            }
        })

        // We need to call this here to make sure the adapters are present, but not attached yet so the position gets
        // restored automatically.
        super.onViewCreated(view, savedInstanceState)

        contentContainer.visibility = View.VISIBLE

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        recyclerView.endScrolls()
                .bindToLifecycle(this)
                .throttleFirst(300, TimeUnit.MILLISECONDS)
                .subscribe { viewModel.loadIfPossible() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun showData(data: List<T>) {
        super.showData(data)

        if (innerAdapter.isEmpty()) {
            innerAdapter.swapData(data)
            innerAdapter.notifyItemRangeInserted(0, data.size)
        } else {
            Single.fromCallable { DiffUtil.calculateDiff(innerAdapter.provideDiffUtilCallback(data)) }
                    .bindToLifecycle(this)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { it: DiffUtil.DiffResult ->
                        innerAdapter.swapData(data)

                        it.dispatchUpdatesTo(adapter)
                    }
        }
    }

    override fun hideData() = Unit

    override fun showError(action: ErrorAction) {
        if (adapter.footer == null) {
            adapter.footer = LayoutInflater.from(context).inflate(R.layout.layout_error, root, false).apply {
                layoutParams.height = when (innerAdapter.itemCount <= 0) {
                    true -> ViewGroup.LayoutParams.MATCH_PARENT
                    false -> ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
        }

        super.showError(action)
    }

    override fun hideError() {
        adapter.footer = null
    }
}
