package me.proxer.app.log

import android.Manifest
import android.arch.lifecycle.Observer
import android.content.ClipData
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.tbruyelle.rxpermissions2.RxPermissions
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.clipboardManager
import me.proxer.app.util.extension.isAtTop
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.unsafeLazy
import org.jetbrains.anko.toast
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class LogActivity : BaseActivity() {

    private val viewModel by unsafeLazy { LogViewModelProvider.get(this) }

    private var adapter by Delegates.notNull<LogAdapter>()

    private val root: ViewGroup by bindView(R.id.root)
    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_log)
        setSupportActionBar(toolbar)

        adapter = LogAdapter(savedInstanceState)

        adapter.longClickSubject
                .autoDispose(this)
                .subscribe {
                    getString(R.string.clipboard_title).let { title ->
                        clipboardManager.primaryClip = ClipData.newPlainText(title, it.toString())
                        toast(R.string.clipboard_status)
                    }
                }

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        recyclerView.adapter = adapter

        viewModel.data.observe(this, Observer {
            it?.let {
                val wasAtTop = recyclerView.layoutManager.isAtTop()

                adapter.swapDataAndNotifyWithDiffing(it)

                if (wasAtTop) {
                    recyclerView.postDelayed({
                        recyclerView.smoothScrollToPosition(0)
                    }, 50)
                }
            }
        })

        viewModel.saveSuccess.observe(this, Observer {
            it?.let { snackbar(root, R.string.activity_log_save_success) }
        })

        viewModel.saveError.observe(this, Observer {
            it?.let { snackbar(root, R.string.activity_log_save_error) }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_log, menu, true)

        menu.findItem(R.id.action_save)
                .clicks()
                .compose(RxPermissions(this).ensure(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .autoDispose(this)
                .subscribe { granted -> if (granted) viewModel.save() }

        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }
}
