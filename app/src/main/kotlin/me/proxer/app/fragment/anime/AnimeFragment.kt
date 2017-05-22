package me.proxer.app.fragment.anime

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.rubengees.ktask.android.AndroidLifecycleTask
import com.rubengees.ktask.android.bindToLifecycle
import com.rubengees.ktask.base.MultiBranchTask.PartialTaskException
import com.rubengees.ktask.operation.CacheTask
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.AnimeActivity
import me.proxer.app.activity.ProfileActivity
import me.proxer.app.activity.TranslatorGroupActivity
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.adapter.anime.StreamAdapter
import me.proxer.app.adapter.base.BaseAdapter
import me.proxer.app.application.GlideApp
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.anime.AnimeFragment.StreamInfo
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.task.proxerTask
import me.proxer.app.task.stream.StreamResolutionTask
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionInput
import me.proxer.app.task.stream.StreamResolutionTask.StreamResolutionResult
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.toMediaLanguage
import me.proxer.app.view.MediaControlView
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.anime.Stream
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category
import okhttp3.HttpUrl
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class AnimeFragment : LoadingFragment<Pair<ProxerCall<List<Stream>>, ProxerCall<EntryCore>>, StreamInfo>() {

    companion object {
        fun newInstance(): AnimeFragment {
            return AnimeFragment().apply {
                arguments = bundleOf()
            }
        }
    }

    override val isWorking: Boolean
        get() = super.isWorking && streamTask.isWorking

    private val animeActivity
        get() = activity as AnimeActivity

    private val id: String
        get() = animeActivity.id

    private var episode: Int
        get() = animeActivity.episode
        set(value) {
            animeActivity.episode = value
        }

    private val language: AnimeLanguage
        get() = animeActivity.language

    private var name: String?
        get() = animeActivity.name
        set(value) {
            animeActivity.name = value
        }

    private var episodeAmount: Int?
        get() = animeActivity.episodeAmount
        set(value) {
            animeActivity.episodeAmount = value
        }

    private lateinit var innerAdapter: StreamAdapter
    private lateinit var adapter: EasyHeaderFooterAdapter

    private lateinit var bookmarkTask: AndroidLifecycleTask<ProxerCall<Void?>, Void?>
    private lateinit var streamTask: AndroidLifecycleTask<StreamResolutionInput, StreamResolutionResult>

    private lateinit var header: MediaControlView

    private val list: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = StreamAdapter(savedInstanceState, GlideApp.with(this))
        adapter = EasyHeaderFooterAdapter(innerAdapter)

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

        streamTask = TaskBuilder.proxerTask<String>()
                .mapInput { it: StreamResolutionInput ->
                    api.anime().link(it.data).build()
                }
                .inputEcho()
                .then(StreamResolutionTask(), mapFunction = {
                    StreamResolutionInput(it.first.name, it.second)
                })
                .async()
                .validateBefore {
                    Validators.validateHosterSupported(it.name)
                }
                .bindToLifecycle(this, "${javaClass}_stream_task")
                .onInnerStart {
                    setProgressVisible(true)
                }
                .onSuccess {
                    if (it.intent.action == Intent.ACTION_VIEW) {
                        if (it.intent.type == "text/html") {
                            showPage(HttpUrl.parse(it.intent.data.toString()) ?: throw NullPointerException())
                        } else {
                            try {
                                context.startActivity(it.intent)
                            } catch (exception: ActivityNotFoundException) {
                                it.notFoundAction.invoke(activity as AppCompatActivity)
                            }
                        }
                    } else {
                        multilineSnackbar(root, it.intent.getCharSequenceExtra(StreamResolutionResult.MESSAGE_EXTRA))
                                .apply {
                                    view.applyRecursively {
                                        if (it is TextView && it !is Button) {
                                            it.movementMethod = LinkMovementMethod.getInstance()
                                        }
                                    }
                                }
                    }
                }
                .onError {
                    ErrorUtils.handle(activity as MainActivity, it).let {
                        multilineSnackbar(root, it.message, Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction)
                    }
                }
                .onFinish {
                    setProgressVisible(isWorking)
                }
                .build()
    }

    override fun onDestroy() {
        innerAdapter.destroy()

        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        header = inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView

        header.textResolver = object : MediaControlView.TextResourceResolver {
            override fun next() = context.getString(R.string.fragment_anime_next_episode)
            override fun previous() = context.getString(R.string.fragment_anime_previous_episode)
            override fun bookmarkThis() = context.getString(R.string.fragment_anime_bookmark_this_episode)
            override fun bookmarkNext() = context.getString(R.string.fragment_anime_bookmark_next_episode)
        }

        return inflater.inflate(R.layout.fragment_anime, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.positionResolver = object : BaseAdapter.PositionResolver() {
            override fun resolveRealPosition(position: Int) = adapter.getRealPosition(position)
        }

        innerAdapter.callback = object : StreamAdapter.StreamAdapterCallback {
            override fun onUploaderClick(item: Stream) {
                ProfileActivity.navigateTo(activity, item.uploaderId, item.uploaderName)
            }

            override fun onTranslatorGroupClick(item: Stream) {
                item.translatorGroupId?.let { id ->
                    item.translatorGroupName?.let { name ->
                        TranslatorGroupActivity.navigateTo(activity, id, name)
                    }
                }
            }

            override fun onWatchClick(item: Stream) {
                streamTask.forceExecute(StreamResolutionInput(item.hosterName, item.id))
            }
        }

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter

        header.callback = object : MediaControlView.MediaControlViewCallback {
            override fun onSetBookmarkClick(episode: Int) {
                bookmarkTask.forceExecute(api.ucp()
                        .setBookmark(id, episode, language.toMediaLanguage(), Category.ANIME)
                        .build())
            }

            override fun onSwitchEpisodeClick(newEpisode: Int) {
                switchEpisode(newEpisode)
            }

            override fun onFinishClick(episode: Int) {
                bookmarkTask.forceExecute(api.info().markAsFinished(id).build())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun freshLoad() {
        adapter.removeHeader()
        innerAdapter.clear()
        streamTask.reset()
        state.clear()

        task.forceExecute(constructInput())
    }

    override fun onSuccess(result: StreamInfo) {
        episodeAmount = result.episodeAmount
        name = result.name

        header.setEpisodeInfo(result.episodeAmount, episode)
        innerAdapter.replace(result.streams)
        adapter.header = header

        super.onSuccess(result)
    }

    override fun showContent() {
        super.showContent()

        if (innerAdapter.isEmpty()) {
            showError(R.string.error_no_data_anime, ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE)
        }
    }

    override fun handleError(error: Throwable): ErrorUtils.ErrorAction {
        if (error is PartialTaskException) {
            if (error.partialResult is EntryCore) {
                episodeAmount = (error.partialResult as EntryCore).episodeAmount
                name = (error.partialResult as EntryCore).name

                episodeAmount?.let {
                    header.setEpisodeInfo(it, episode)
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

    override fun constructTask() = TaskBuilder.proxerTask<List<Stream>>()
            .parallelWith(TaskBuilder.proxerTask<EntryCore>().cache(CacheTask.CacheStrategy.RESULT),
                    zipFunction = { streams, info -> StreamInfo(streams, info.name, info.episodeAmount) },
                    awaitRightResultOnError = true).build()

    override fun constructInput() = api.anime()
            .streams(id, episode, language)
            .includeProxerStreams(true)
            .build() to api.info()
            .entryCore(id).build()

    private fun switchEpisode(newEpisode: Int) {
        episode = newEpisode

        freshLoad()
    }

    class StreamInfo(val streams: List<Stream>, val name: String, val episodeAmount: Int)
}
