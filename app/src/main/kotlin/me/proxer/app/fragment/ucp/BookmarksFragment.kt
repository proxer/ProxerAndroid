package me.proxer.app.fragment.ucp

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.mikepenz.iconics.utils.IconicsMenuInflatorUtil
import com.rubengees.ktask.android.AndroidLifecycleTask
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.AnimeActivity
import me.proxer.app.activity.MangaActivity
import me.proxer.app.activity.MediaActivity
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.adapter.ucp.BookmarkAdapter
import me.proxer.app.application.GlideApp
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.ucp.Bookmark
import me.proxer.library.enums.Category
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class BookmarksFragment : PagedLoadingFragment<ProxerCall<List<Bookmark>>, Bookmark>() {

    companion object {
        private const val CATEGORY_ARGUMENT = "category"

        fun newInstance(): BookmarksFragment {
            return BookmarksFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val isLoginRequired = true
    override val isSwipeToRefreshEnabled = true
    override val shouldReplaceOnRefresh = true
    override val emptyResultMessage = R.string.error_no_data_bookmarks
    override val itemsOnPage = 30

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1, StaggeredGridLayoutManager.VERTICAL)
    }

    override val innerAdapter by lazy { BookmarkAdapter(GlideApp.with(this)) }

    private lateinit var removalTask: AndroidLifecycleTask<ProxerCall<Void?>, Void?>
    private val removalQueue = LinkedHashSet<Bookmark>()

    private var category: Category?
        get() = arguments.getSerializable(CATEGORY_ARGUMENT) as? Category
        set(value) = arguments.putSerializable(CATEGORY_ARGUMENT, value)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        removalTask = TaskBuilder.asyncProxerTask<Void?>()
                .validateBefore {
                    Validators.validateLogin()
                }
                .bindToLifecycle(this, "${javaClass}_removal_task")
                .onSuccess {
                    innerAdapter.remove(removalQueue.first())
                    removalQueue.remove(removalQueue.first())

                    saveResultToState(innerAdapter.list)
                    removeBookmarksFromQueue()
                }
                .onError {
                    removalQueue.clear()

                    ErrorUtils.handle(activity as MainActivity, it).let {
                        multilineSnackbar(root, getString(R.string.error_bookmark_removal, getString(it.message)),
                                Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction)
                    }
                }.build()

        innerAdapter.callback = object : BookmarkAdapter.BookmarkAdapterCallback {
            override fun onBookmarkClick(item: Bookmark) {
                when (item.category) {
                    Category.ANIME -> AnimeActivity.navigateTo(activity, item.entryId,
                            item.episode, item.language.toAnimeLanguage(), item.name)
                    Category.MANGA -> MangaActivity.navigateTo(activity, item.entryId,
                            item.episode, item.language.toGeneralLanguage(), item.chapterName, item.name)
                }
            }

            override fun onBookmarkLongClick(view: View, item: Bookmark) {
                val imageView = view.find<ImageView>(R.id.image)

                MediaActivity.navigateTo(activity, item.entryId, item.name, item.category,
                        if (imageView.drawable != null) imageView else null)
            }

            override fun onBookmarkRemoval(item: Bookmark) {
                removalQueue.add(item)

                removeBookmarksFromQueue()
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onDestroy() {
        innerAdapter.destroy()

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflatorUtil.inflate(inflater, context, R.menu.fragment_bookmarks, menu, true)

        when (category) {
            Category.ANIME -> menu.findItem(R.id.anime).isChecked = true
            Category.MANGA -> menu.findItem(R.id.manga).isChecked = true
            else -> menu.findItem(R.id.all).isChecked = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val previousCategory = category

        when (item.itemId) {
            R.id.anime -> category = Category.ANIME
            R.id.manga -> category = Category.MANGA
            R.id.all -> category = null
        }

        if (category != previousCategory) {
            item.isChecked = true

            freshLoad()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<List<Bookmark>>().build()
    override fun constructPagedInput(page: Int) = api.ucp().bookmarks()
            .category(category)
            .page(page)
            .limit(itemsOnPage)
            .build()

    private fun removeBookmarksFromQueue() {
        if (removalQueue.isNotEmpty()) {
            removalTask.execute(api.ucp().deleteBookmark(removalQueue.first().id).build())
        }
    }
}
