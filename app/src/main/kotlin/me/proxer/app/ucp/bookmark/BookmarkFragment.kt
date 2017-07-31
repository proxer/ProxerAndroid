package me.proxer.app.ucp.bookmark

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.StaggeredGridLayoutManager.VERTICAL
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.library.entitiy.ucp.Bookmark
import me.proxer.library.enums.Category
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class BookmarkFragment : PagedContentFragment<Bookmark>() {

    companion object {
        private const val CATEGORY_ARGUMENT = "category"

        fun newInstance() = BookmarkFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage = R.string.error_no_data_bookmark

    override val viewModel: BookmarkViewModel by lazy {
        ViewModelProviders.of(this).get(BookmarkViewModel::class.java)
    }

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1, VERTICAL)
    }

    override lateinit var innerAdapter: BookmarkAdapter

    private var category: Category?
        get() = arguments.getSerializable(CATEGORY_ARGUMENT) as? Category
        set(value) {
            arguments.putSerializable(CATEGORY_ARGUMENT, value)

            viewModel.setCategory(value)
        }

    private val toolbar by lazy { activity.findViewById<Toolbar>(R.id.toolbar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = BookmarkAdapter(GlideApp.with(this))

        viewModel.itemRemovalError.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, getString(R.string.fragment_set_user_info_error, getString(it.message)),
                        Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction?.toClickListener(hostingActivity))

                viewModel.itemRemovalError.value = null
            }
        })

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.clickSubject
                .bindToLifecycle(this)
                .subscribe {
                    // TODO
                }

        innerAdapter.longClickSubject
                .bindToLifecycle(this)
                .subscribe { (view, bookmark) ->
                    MediaActivity.navigateTo(activity, bookmark.entryId, bookmark.name, bookmark.category,
                            if (view.drawable != null) view else null)
                }

        innerAdapter.removeClickSubject
                .bindToLifecycle(this)
                .subscribe {
                    viewModel.addItemToRemove(it)
                }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_bookmarks, menu, true)

        when (category) {
            Category.ANIME -> menu.findItem(R.id.anime).isChecked = true
            Category.MANGA -> menu.findItem(R.id.manga).isChecked = true
            else -> menu.findItem(R.id.all).isChecked = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.anime -> category = Category.ANIME
            R.id.manga -> category = Category.MANGA
            R.id.all -> category = null
        }

        item.isChecked = true

        return super.onOptionsItemSelected(item)
    }
}