package me.proxer.app.fragment.manga

import android.content.res.Resources
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.transition.TransitionManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.iconics.utils.IconicsMenuInflatorUtil
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.rubengees.ktask.android.AndroidLifecycleTask
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.util.TaskBuilder
import com.rubengees.ktask.util.WorkerTask
import me.proxer.app.R
import me.proxer.app.activity.MangaActivity
import me.proxer.app.activity.MediaActivity
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.adapter.base.BaseAdapter
import me.proxer.app.adapter.manga.LocalMangaAdapter
import me.proxer.app.application.GlideApp
import me.proxer.app.entity.manga.LocalMangaChapter
import me.proxer.app.event.manga.LocalMangaJobFailedEvent
import me.proxer.app.event.manga.LocalMangaJobFinishedEvent
import me.proxer.app.event.manga.LocalMangaJobStartedEvent
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.job.LocalMangaJob
import me.proxer.app.task.manga.LocalMangaListTask
import me.proxer.app.task.manga.MangaChapterRemovalTask
import me.proxer.app.task.manga.MangaChapterRemovalTask.MangaChapterRemovalTaskInput
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

/**
 * @author Ruben Gees
 */
class LocalMangaFragment : LoadingFragment<Unit, List<CompleteLocalMangaEntry>>() {

    companion object {
        private const val SEARCH_QUERY_ARGUMENT = "search_query"

        fun newInstance(): LocalMangaFragment {
            return LocalMangaFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val isLoginRequired = true

    private lateinit var removalTask: AndroidLifecycleTask<MangaChapterRemovalTaskInput, Unit>
    private lateinit var jobStateUpdateTask: AndroidLifecycleTask<Resources, String>

    private lateinit var innerAdapter: LocalMangaAdapter
    private lateinit var adapter: EasyHeaderFooterAdapter

    private lateinit var header: ViewGroup
    private lateinit var headerText: TextView
    private lateinit var headerCancel: Button

    private lateinit var searchItem: MenuItem
    private lateinit var searchView: SearchView

    private var searchQuery: String
        get() = arguments.getString(SEARCH_QUERY_ARGUMENT, "")
        set(value) = arguments.putString(SEARCH_QUERY_ARGUMENT, value)

    private val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        removalTask = TaskBuilder.task(MangaChapterRemovalTask())
                .async()
                .validateBefore { validate() }
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

        jobStateUpdateTask = TaskBuilder.task(JobStateUpdateTask())
                .async()
                .validateBefore { validate() }
                .bindToLifecycle(this, "${javaClass}_job_state_update_task")
                .onSuccess {
                    if (it.isNotEmpty()) {
                        headerText.text = it

                        adapter.header = header
                    } else {
                        adapter.header = null
                    }

                    showContent()
                }
                .build()

        innerAdapter = LocalMangaAdapter(savedInstanceState, GlideApp.with(this))
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        innerAdapter.positionResolver = object : BaseAdapter.PositionResolver() {
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
                removalTask.execute(MangaChapterRemovalTaskInput(entry, chapter))
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        jobStateUpdateTask.forceExecute(resources)
    }

    override fun onDestroyOptionsMenu() {
        searchView.setOnQueryTextListener(null)
        searchItem.setOnActionExpandListener(null)

        super.onDestroyOptionsMenu()
    }

    override fun onDestroy() {
        innerAdapter.destroy()

        super.onDestroy()
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

                jobStateUpdateTask.forceExecute(resources)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflatorUtil.inflate(inflater, context, R.menu.fragment_local_manga, menu, true)

        searchItem = menu.findItem(R.id.search)
        searchView = searchItem.actionView as SearchView

        if (searchQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(searchQuery, false)
            searchView.clearFocus()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                searchQuery = newText

                freshLoad()

                return false
            }
        })

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                TransitionManager.beginDelayedTransition(activity.find(R.id.toolbar))

                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                searchQuery = ""

                freshLoad()

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

    override fun onSuccess(result: List<CompleteLocalMangaEntry>) {
        innerAdapter.replace(result)

        super.onSuccess(result)
    }

    override fun onError(error: Throwable) {
        super.onError(error)

        innerAdapter.clear()
        contentContainer.visibility = View.GONE
    }

    override fun hideContent() {
        // Don't do anything here, we want to keep showing the current content.
    }

    override fun showContent() {
        super.showContent()

        if (innerAdapter.isEmpty() && !adapter.hasHeader()) {
            if (searchQuery.isEmpty()) {
                showError(R.string.error_no_data_local_manga, ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE)
            } else {
                showError(R.string.error_no_data_search, ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE)
            }
        } else {
            hideError()
        }
    }

    override fun constructInput() = Unit
    override fun constructTask() = TaskBuilder.task(LocalMangaListTask())
            .map { it.filter { searchQuery.isEmpty() || it.first.name.contains(searchQuery, true) } }
            .async()
            .build()

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocalMangaJobStarted(@Suppress("UNUSED_PARAMETER") event: LocalMangaJobStartedEvent) {
        jobStateUpdateTask.forceExecute(resources)
        freshLoad()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocalMangaJobFinished(@Suppress("UNUSED_PARAMETER") event: LocalMangaJobFinishedEvent) {
        jobStateUpdateTask.forceExecute(resources)
        freshLoad()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocalMangaJobFailed(@Suppress("UNUSED_PARAMETER") event: LocalMangaJobFailedEvent) {
        jobStateUpdateTask.forceExecute(resources)
    }

    internal class JobStateUpdateTask : WorkerTask<Resources, String>() {
        override fun work(input: Resources): String {
            val runningJobs = LocalMangaJob.countRunningJobs()
            val scheduledJobs = LocalMangaJob.countScheduledJobs()
            var message = ""

            message += when (runningJobs > 0) {
                true -> input.getQuantityString(R.plurals.fragment_local_manga_chapters_downloading,
                        runningJobs, runningJobs)
                false -> ""
            }

            message += when (runningJobs > 0 && scheduledJobs > 0) {
                true -> "\n"
                false -> ""
            }

            message += when (scheduledJobs > 0) {
                true -> input.getQuantityString(R.plurals.fragment_local_manga_chapters_scheduled,
                        scheduledJobs, scheduledJobs)
                false -> ""
            }

            return message
        }
    }
}
