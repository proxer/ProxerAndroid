package me.proxer.app.fragment.manga

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.transition.TransitionManager
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.rubengees.ktask.android.AndroidLifecycleTask
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.R
import me.proxer.app.activity.MangaActivity
import me.proxer.app.activity.MediaActivity
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.adapter.manga.LocalMangaAdapter
import me.proxer.app.application.MainApplication.Companion.mangaDb
import me.proxer.app.entity.LocalMangaChapter
import me.proxer.app.event.LocalMangaJobFailedEvent
import me.proxer.app.event.LocalMangaJobFinishedEvent
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.job.LocalMangaJob
import me.proxer.app.task.manga.LocalMangaListTask
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.CompleteLocalMangaEntry
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.enums.Category
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.uiThread
import java.util.concurrent.Future

/**
 * @author Ruben Gees
 */
class LocalMangaFragment : LoadingFragment<Unit, List<CompleteLocalMangaEntry>>() {

    companion object {
        fun newInstance(): LocalMangaFragment {
            return LocalMangaFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val isLoginRequired = true

    private lateinit var removalTask: AndroidLifecycleTask<Pair<EntryCore, LocalMangaChapter>, Unit>

    private lateinit var innerAdapter: LocalMangaAdapter
    private lateinit var adapter: EasyHeaderFooterAdapter

    private lateinit var header: ViewGroup
    private lateinit var headerText: TextView
    private lateinit var headerCancel: Button

    private val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        removalTask = TaskBuilder.task(ChapterRemovalTask())
                .bindToLifecycle(this, "${javaClass}_removal_task")
                .onSuccess {
                    freshLoad()
                }
                .onError {
                    ErrorUtils.handle(activity as MainActivity, it).let {
                        multilineSnackbar(root, getString(R.string.error_local_manga_removal, getString(it.message)),
                                Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction)
                    }
                }.build()

        innerAdapter = LocalMangaAdapter(savedInstanceState, Glide.with(this))
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        innerAdapter.positionResolver = object : PagingAdapter.PositionResolver() {
            override fun resolveRealPosition(position: Int) = adapter.getRealPosition(position)
        }

        innerAdapter.callback = object : LocalMangaAdapter.LocalMangaAdapterCallback {
            override fun onChapterClick(entry: EntryCore, chapter: LocalMangaChapter) {
                MangaActivity.navigateTo(activity, entry.id, chapter.episode, chapter.language, chapter.title,
                        entry.name, entry.episodeAmount)
            }

            override fun onChapterLongClick(view: View, entry: EntryCore) {
                val imageView = view.find<ImageView>(R.id.image)

                MediaActivity.navigateTo(activity, entry.id, entry.name, Category.MANGA,
                        if (imageView.drawable != null) imageView else null)
            }

            override fun onDeleteClick(entry: EntryCore, chapter: LocalMangaChapter) {
                removalTask.execute(entry to chapter)
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        updateMangaJobState()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        header = inflater.inflate(R.layout.layout_local_manga_header, container, false) as ViewGroup
        headerText = header.find(R.id.text)
        headerCancel = header.find(R.id.cancel)

        return inflater.inflate(R.layout.fragment_local_manga, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(activity) + 1,
                StaggeredGridLayoutManager.VERTICAL)
        list.adapter = adapter

        headerCancel.setOnClickListener {
            doAsync {
                LocalMangaJob.cancelAll()

                uiThread {
                    updateMangaJobState()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_local_manga, menu)

        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                state.data?.let {
                    if (newText.isNotEmpty()) {
                        innerAdapter.replace(it.filter { it.first.name.contains(newText, true) })
                    } else {
                        innerAdapter.replace(it)
                    }
                }

                return false
            }
        })

        MenuItemCompat.setOnActionExpandListener(searchItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                TransitionManager.beginDelayedTransition(activity.find(R.id.toolbar))

                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                state.data?.let {
                    innerAdapter.replace(it)
                }

                TransitionManager.beginDelayedTransition(activity.find(R.id.toolbar))

                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun hideContent() {
        // Don't do anything here, we want to keep showing the current content.
    }

    override fun onSuccess(result: List<CompleteLocalMangaEntry>) {
        innerAdapter.replace(result)

        super.onSuccess(result)
    }

    override fun showContent() {
        super.showContent()

        if (innerAdapter.isEmpty()) {
            showError(R.string.error_no_data_local_manga, ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE)
        }
    }

    override fun constructInput() = Unit
    override fun constructTask() = TaskBuilder.task(LocalMangaListTask())
            .async()
            .build()

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocalMangaJobFinished(@Suppress("UNUSED_PARAMETER") event: LocalMangaJobFinishedEvent) {
        updateMangaJobState()
        freshLoad()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocalMangaJobFailed(@Suppress("UNUSED_PARAMETER") event: LocalMangaJobFailedEvent) {
        updateMangaJobState()
    }

    private fun updateMangaJobState() {
        header.tag.let {
            if (it is Future<*>) {
                it.cancel(true)
            }
        }

        header.tag = doAsync {
            val runningJobs = LocalMangaJob.countRunningJobs()
            val scheduledJobs = LocalMangaJob.countScheduledJobs()
            var message = ""

            message += when (runningJobs > 0) {
                true -> context.resources.getQuantityString(R.plurals.fragment_local_manga_chapters_downloading,
                        runningJobs, runningJobs)
                false -> ""
            }

            message += when (runningJobs > 0 && scheduledJobs > 0) {
                true -> "; "
                false -> ""
            }

            message += when (scheduledJobs > 0) {
                true -> context.resources.getQuantityString(R.plurals.fragment_local_manga_chapters_scheduled,
                        scheduledJobs, scheduledJobs)
                false -> ""
            }

            uiThread {
                if (message.isNotEmpty()) {
                    headerText.text = message

                    adapter.header = header
                } else {
                    adapter.header = null
                }
            }
        }
    }

    private class ChapterRemovalTask : WorkerTask<Pair<EntryCore, LocalMangaChapter>, Unit>() {
        override fun work(input: Pair<EntryCore, LocalMangaChapter>) = mangaDb.removeChapter(input.first, input.second)
    }
}
