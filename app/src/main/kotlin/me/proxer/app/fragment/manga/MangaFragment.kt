package me.proxer.app.fragment.manga

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.rubengees.ktask.android.AndroidLifecycleTask
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.base.MultiBranchTask.PartialTaskException
import com.rubengees.ktask.base.Task
import com.rubengees.ktask.operation.CacheTask
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.MangaActivity
import me.proxer.app.activity.ProfileActivity
import me.proxer.app.activity.TranslatorGroupActivity
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.adapter.manga.MangaAdapter
import me.proxer.app.application.MainApplication
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.entity.MangaChapterInfo
import me.proxer.app.entity.MangaInput
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.task.manga.LocalMangaChapterTask
import me.proxer.app.task.manga.LocalMangaEntryTask
import me.proxer.app.task.manga.MangaCleanTask
import me.proxer.app.task.proxerTask
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.toMediaLanguage
import me.proxer.app.view.MediaControlView
import me.proxer.app.view.MediaControlView.SimpleTranslatorGroup
import me.proxer.app.view.MediaControlView.Uploader
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.entitiy.manga.Chapter
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find

/**
 * @author Ruben Gees
 */
class MangaFragment : LoadingFragment<MangaInput, MangaChapterInfo>() {

    companion object {
        fun newInstance(): MangaFragment {
            return MangaFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    private val mangaActivity
        get() = activity as MangaActivity

    private val id: String
        get() = mangaActivity.id

    private var episode: Int
        get() = mangaActivity.episode
        set(value) {
            mangaActivity.episode = value
        }

    private val language: Language
        get() = mangaActivity.language

    private var chapterTitle: String?
        get() = mangaActivity.chapterTitle
        set(value) {
            mangaActivity.chapterTitle = value
        }

    private var name: String?
        get() = mangaActivity.name
        set(value) {
            mangaActivity.name = value
        }

    private var episodeAmount: Int?
        get() = mangaActivity.episodeAmount
        set(value) {
            mangaActivity.episodeAmount = value
        }

    private val innerAdapter by lazy { MangaAdapter() }
    private val adapter by lazy { EasyHeaderFooterAdapter(innerAdapter) }

    private lateinit var bookmarkTask: AndroidLifecycleTask<ProxerCall<Void?>, Void?>

    private lateinit var header: MediaControlView
    private lateinit var footer: ViewGroup

    private lateinit var scrollToTop: FloatingActionButton
    private val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bookmarkTask = TaskBuilder.asyncProxerTask<Void?>()
                .validateBefore {
                    Validators.validateLogin()
                }
                .bindToLifecycle(this, "${javaClass}_bookmark_task")
                .onSuccess {
                    snackbar(root, R.string.fragment_set_user_info_success)
                }
                .onError { it ->
                    ErrorUtils.handle(activity as MainActivity, it).let {
                        multilineSnackbar(root, getString(R.string.fragment_set_user_info_error,
                                getString(it.message)), Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction)
                    }
                }
                .build()

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        header = inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView
        footer = inflater.inflate(R.layout.layout_manga_footer, container, false) as ViewGroup
        scrollToTop = footer.find(R.id.scrollToTop)

        val horizontalMargin = context.resources.getDimensionPixelSize(R.dimen.screen_horizontal_margin_with_items)
        val verticalMargin = context.resources.getDimensionPixelSize(R.dimen.screen_vertical_margin_with_items)

        (header.layoutParams as ViewGroup.MarginLayoutParams).setMargins(horizontalMargin, verticalMargin,
                horizontalMargin, verticalMargin)

        header.textResolver = object : MediaControlView.TextResourceResolver {
            override fun next() = context.getString(R.string.fragment_manga_next_chapter)
            override fun previous() = context.getString(R.string.fragment_manga_previous_chapter)
            override fun bookmarkThis() = context.getString(R.string.fragment_manga_bookmark_this_chapter)
            override fun bookmarkNext() = context.getString(R.string.fragment_manga_bookmark_next_chapter)
        }

        return inflater.inflate(R.layout.fragment_manga, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.VISIBLE) {
                    (activity as AppCompatActivity).supportActionBar?.show()
                } else {
                    (activity as AppCompatActivity).supportActionBar?.hide()
                }
            }
        }

        innerAdapter.positionResolver = object : PagingAdapter.PositionResolver() {
            override fun resolveRealPosition(position: Int) = adapter.getRealPosition(position)
        }

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter

        header.callback = object : MediaControlView.MediaControlViewCallback {
            override fun onUploaderClick(uploader: Uploader) {
                ProfileActivity.navigateTo(activity, uploader.id, uploader.name)
            }

            override fun onTranslatorGroupClick(group: SimpleTranslatorGroup) {
                TranslatorGroupActivity.navigateTo(activity, group.id, group.name)
            }

            override fun onSwitchEpisodeClick(newEpisode: Int) {
                switchEpisode(newEpisode)
            }

            override fun onSetBookmarkClick(episode: Int) {
                bookmarkTask.forceExecute(api.ucp()
                        .setBookmark(id, episode, language.toMediaLanguage(), Category.MANGA)
                        .build())
            }

            override fun onFinishClick(episode: Int) {
                bookmarkTask.forceExecute(api.info().markAsFinished(id).build())
            }
        }

        scrollToTop.setImageDrawable(IconicsDrawable(context)
                .icon(CommunityMaterial.Icon.cmd_chevron_up)
                .sizeDp(56)
                .paddingDp(8)
                .colorRes(android.R.color.white))

        scrollToTop.setOnClickListener {
            list.scrollToPosition(0)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_manga, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.fullscreen -> {
                val isFullscreen = activity.window.decorView.systemUiVisibility and
                        View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN

                if (isFullscreen) {
                    showSystemUI()
                } else {
                    hideSystemUI()
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun freshLoad() {
        adapter.removeHeader()
        adapter.removeFooter()
        innerAdapter.clear()
        state.clear()

        task.forceExecute(constructInput())
    }

    override fun onSuccess(result: MangaChapterInfo) {
        chapterTitle = result.chapter.title
        episodeAmount = result.episodeAmount
        name = result.name

        header.setEpisodeInfo(result.episodeAmount, episode)
        header.setDateTime(TimeUtils.convertToDateTime(result.chapter.date))
        header.setUploader(Uploader(result.chapter.uploaderId, result.chapter.uploaderName))

        result.chapter.scanGroupId?.let { id ->
            result.chapter.scanGroupName?.let { name ->
                header.setTranslatorGroup(SimpleTranslatorGroup(id, name))
            }
        }

        adapter.header = header
        adapter.footer = footer

        innerAdapter.init(result.chapter.server, result.chapter.entryId, result.chapter.id)
        innerAdapter.replace(result.chapter.pages)

        super.onSuccess(result)
    }

    override fun onError(error: Throwable) {
        super.onError(error)

        chapterTitle = null
    }

    override fun handleError(error: Throwable): ErrorUtils.ErrorAction {
        if (error is PartialTaskException) {
            if (error.partialResult is EntryCore) {
                episodeAmount = (error.partialResult as EntryCore).episodeAmount
                name = (error.partialResult as EntryCore).name

                episodeAmount?.let {
                    header.setEpisodeInfo(it, episode)
                    header.setTranslatorGroup(null)
                    header.setUploader(null)
                    header.setDateTime(null)

                    adapter.header = header
                }
            }
        }

        return super.handleError(error)
    }

    override fun showError(message: Int, buttonMessage: Int, buttonAction: View.OnClickListener?) {
        super.showError(message, buttonMessage, buttonAction)

        if (adapter.hasHeader()) {
            contentContainer.visibility = View.VISIBLE
            errorContainer.visibility = View.INVISIBLE

            errorInnerContainer.post {
                val newCenter = root.height / 2f + header.height / 2f
                val containerCenterCorrection = errorInnerContainer.height / 2f

                errorInnerContainer.y = newCenter - containerCenterCorrection
                errorContainer.visibility = View.VISIBLE
            }
        } else {
            errorContainer.translationY = 0f
        }
    }

    override fun constructInput() = MangaInput(id, episode, language)
    override fun constructTask(): Task<MangaInput, MangaChapterInfo> {
        val proxerChapterTask = TaskBuilder.proxerTask<Chapter>().mapInput<MangaInput> {
            MainApplication.api.manga()
                    .chapter(it.id, it.episode, it.language)
                    .build()
        }.build()

        val proxerEntryCoreTask = TaskBuilder.proxerTask<EntryCore>().mapInput<String> {
            MainApplication.api.info()
                    .entryCore(it)
                    .build()
        }.build()

        val localChapterTask = TaskBuilder.attemptTask(LocalMangaChapterTask(), proxerChapterTask)
        val localEntryTask = TaskBuilder.attemptTask(LocalMangaEntryTask(), proxerEntryCoreTask)
                .cache(CacheTask.CacheStrategy.RESULT)

        val mangaTask = localChapterTask.parallelWith(localEntryTask, zipFunction = { chapter, entry ->
            MangaChapterInfo(chapter, entry.name, entry.episodeAmount)
        }, awaitLeftResultOnError = true)

        return TaskBuilder.task(MangaCleanTask(context.filesDir))
                .then(mangaTask.mapInput<MangaInput> { it to it.id })
                .async()
                .build()
    }

    private fun switchEpisode(newEpisode: Int) {
        episode = newEpisode

        freshLoad()
    }

    @SuppressLint("InlinedApi")
    private fun showSystemUI() {
        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE
        } else {
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}
