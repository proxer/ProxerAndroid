package me.proxer.app.fragment.ucp

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.rubengees.ktask.android.AndroidLifecycleTask
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.AnimeActivity
import me.proxer.app.activity.MangaActivity
import me.proxer.app.activity.MediaActivity
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.adapter.ucp.BookmarkAdapter
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.PagedLoadingFragment
import me.proxer.app.task.asyncProxerTask
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
    override val spanCount get() = super.spanCount + 1
    override val itemsOnPage = 30

    override val innerAdapter by lazy { BookmarkAdapter() }

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

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.callback = object : BookmarkAdapter.BookmarkAdapterCallback {
            override fun onBookmarkClick(item: Bookmark) {
                when (item.category) {
                    Category.ANIME -> AnimeActivity.navigateTo(activity, item.entryId,
                            item.episode, item.language.toAnimeLanguage(), item.name)
                    Category.MANGA -> MangaActivity.navigateTo(activity, item.entryId,
                            item.episode, item.language.toGeneralLanguage(), null, item.name)
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
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_bookmarks, menu)

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
