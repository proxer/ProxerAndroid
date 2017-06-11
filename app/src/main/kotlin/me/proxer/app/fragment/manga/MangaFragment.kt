package me.proxer.app.fragment.manga

import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import me.proxer.app.adapter.base.BaseAdapter
import me.proxer.app.adapter.manga.MangaAdapter
import me.proxer.app.application.MainApplication
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.entity.manga.MangaChapterInfo
import me.proxer.app.entity.manga.MangaInput
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.task.manga.LocalMangaChapterTask
import me.proxer.app.task.manga.LocalMangaEntryTask
import me.proxer.app.task.manga.MangaCleanTask
import me.proxer.app.task.proxerTask
import me.proxer.app.util.DeviceUtils
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

    private val mediaControlTextResolver = object : MediaControlView.TextResourceResolver {
        override fun next() = context.getString(R.string.fragment_manga_next_chapter)
        override fun previous() = context.getString(R.string.fragment_manga_previous_chapter)
        override fun bookmarkThis() = context.getString(R.string.fragment_manga_bookmark_this_chapter)
        override fun bookmarkNext() = context.getString(R.string.fragment_manga_bookmark_next_chapter)
    }

    private val mediaControlCallback = object : MediaControlView.MediaControlViewCallback {
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

    private val innerAdapter by lazy { MangaAdapter() }
    private val adapter by lazy { EasyHeaderFooterAdapter(innerAdapter) }

    private lateinit var bookmarkTask: AndroidLifecycleTask<ProxerCall<Void?>, Void?>

    private lateinit var header: MediaControlView
    private lateinit var footer: MediaControlView

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

        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
    }

    override fun onDestroy() {
        innerAdapter.destroy()

        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val horizontalMargin = DeviceUtils.getHorizontalMargin(context, true)
        val verticalMargin = DeviceUtils.getVerticalMargin(context, true)

        header = (inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView).apply {
            textResolver = mediaControlTextResolver
            callback = mediaControlCallback

            (layoutParams as ViewGroup.MarginLayoutParams).setMargins(horizontalMargin, verticalMargin,
                    horizontalMargin, verticalMargin)
        }

        footer = (inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView).apply {
            textResolver = mediaControlTextResolver
            callback = mediaControlCallback

            (layoutParams as ViewGroup.MarginLayoutParams).setMargins(horizontalMargin, verticalMargin,
                    horizontalMargin, verticalMargin)
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

        innerAdapter.positionResolver = object : BaseAdapter.PositionResolver() {
            override fun resolveRealPosition(position: Int) = adapter.getRealPosition(position)
        }

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
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

        footer.setEpisodeInfo(result.episodeAmount, episode)

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
        }, awaitRightResultOnError = true)

        return TaskBuilder.task(MangaCleanTask(context.filesDir))
                .then(mangaTask.mapInput<MangaInput> { it to it.id })
                .async()
                .build()
    }

    private fun switchEpisode(newEpisode: Int) {
        episode = newEpisode

        freshLoad()
    }
}
