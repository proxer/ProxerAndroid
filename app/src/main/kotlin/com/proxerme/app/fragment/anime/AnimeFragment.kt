package com.proxerme.app.fragment.anime

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.proxerme.app.R
import com.proxerme.app.activity.AnimeActivity
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.activity.ProfileActivity
import com.proxerme.app.activity.TranslatorGroupActivity
import com.proxerme.app.adapter.anime.StreamAdapter
import com.proxerme.app.entitiy.EntryInfo
import com.proxerme.app.fragment.anime.AnimeFragment.AnimeInput
import com.proxerme.app.fragment.anime.AnimeFragment.StreamInfo
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.task.EntryInfoTask
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.StreamResolutionTask
import com.proxerme.app.task.StreamResolutionTask.*
import com.proxerme.app.task.framework.*
import com.proxerme.app.util.ErrorUtils
import com.proxerme.app.util.Validators
import com.proxerme.app.util.ViewUtils
import com.proxerme.app.util.bindView
import com.proxerme.app.view.MediaControlView
import com.proxerme.library.connection.ProxerException
import com.proxerme.library.connection.anime.entity.Stream
import com.proxerme.library.connection.anime.request.LinkRequest
import com.proxerme.library.connection.anime.request.StreamsRequest
import com.proxerme.library.connection.info.request.SetUserInfoRequest
import com.proxerme.library.connection.ucp.request.SetReminderRequest
import com.proxerme.library.parameters.CategoryParameter
import com.proxerme.library.parameters.ViewStateParameter
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import okhttp3.HttpUrl
import java.io.IOException

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class AnimeFragment : SingleLoadingFragment<Pair<AnimeInput, String>, StreamInfo>() {

    companion object {
        fun newInstance(): AnimeFragment {
            return AnimeFragment()
        }
    }

    private val reminderSuccess = { _: Void? ->
        if (view != null) {
            Snackbar.make(root, R.string.fragment_set_user_info_success, Snackbar.LENGTH_LONG).show()
        }
    }

    private val reminderException = { exception: Exception ->
        if (view != null) {
            val action = ErrorUtils.handle(activity as MainActivity, exception)

            ViewUtils.makeMultilineSnackbar(root,
                    getString(R.string.fragment_set_user_info_error, action.message),
                    Snackbar.LENGTH_LONG).setAction(action.buttonMessage, action.buttonAction).show()
        }
    }

    private val streamResolverSuccess = { result: StreamResolutionResult ->
        if (view != null) {
            if (result.intent.action == Intent.ACTION_VIEW) {
                if (result.intent.type == "text/html") {
                    showPage(HttpUrl.parse(result.intent.data.toString()))
                } else {
                    try {
                        context.startActivity(result.intent)
                    } catch (exception: ActivityNotFoundException) {
                        result.notFoundAction.invoke(activity as AppCompatActivity)
                    }
                }
            } else {
                ViewUtils.makeMultilineSnackbar(root,
                        result.intent.getStringExtra(StreamResolutionResult.MESSAGE),
                        Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private val streamResolverException = { exception: Exception ->
        if (view != null) {
            when (exception) {
                is NoResolverException -> {
                    Snackbar.make(root, R.string.error_unsupported_hoster, Snackbar.LENGTH_LONG).show()
                }
                is StreamResolutionException -> {
                    Snackbar.make(root, R.string.error_stream_resolution, Snackbar.LENGTH_LONG).show()
                }
                is IOException -> {
                    Snackbar.make(root, R.string.error_network, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    val action = ErrorUtils.handle(activity as MainActivity, exception)

                    Snackbar.make(root, action.message, Snackbar.LENGTH_LONG)
                            .setAction(action.buttonMessage, action.buttonAction).show()
                }
            }
        }
    }

    override val section = SectionManager.Section.ANIME
    override val isWorking: Boolean
        get() = super.isWorking && streamResolverTask.isWorking

    private val animeActivity
        get() = activity as AnimeActivity

    private val id: String
        get() = animeActivity.id
    private var episode: Int
        get() = animeActivity.episode
        set(value) {
            animeActivity.episode = value
        }
    private val language: String
        get() = animeActivity.language
    private var entryInfo: EntryInfo
        get() = animeActivity.entryInfo
        set(value) {
            animeActivity.entryInfo = value
        }

    private lateinit var streamAdapter: StreamAdapter
    private lateinit var adapter: EasyHeaderFooterAdapter

    private val reminderTask = constructReminderTask()
    private val streamResolverTask = constructStreamResolverTask()

    private lateinit var header: MediaControlView

    private val streams: RecyclerView by bindView(R.id.streams)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        streamAdapter = StreamAdapter({ adapter.getRealPosition(it) })
        adapter = EasyHeaderFooterAdapter(streamAdapter)

        streamAdapter.callback = object : StreamAdapter.StreamAdapterCallback() {
            override fun onUploaderClick(item: Stream) {
                ProfileActivity.navigateTo(activity, item.uploaderId, item.uploader)
            }

            override fun onTranslatorGroupClick(item: Stream) {
                item.translatorGroupId?.let { id ->
                    item.translatorGroup?.let { name ->
                        TranslatorGroupActivity.navigateTo(activity, id, name)
                    }
                }
            }

            override fun onWatchClick(item: Stream) {
                streamResolverTask.execute(StreamResolverInput(item.hosterName, item.id))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        header = inflater.inflate(R.layout.item_media_header, container, false) as MediaControlView

        header.textResolver = object : MediaControlView.TextResourceResolver {
            override fun finish() = context.getString(R.string.finished_media)
            override fun next() = context.getString(R.string.fragment_anime_next_episode)
            override fun previous() = context.getString(R.string.fragment_anime_previous_episode)
            override fun reminderThis() =
                    context.getString(R.string.fragment_anime_reminder_this_episode)

            override fun reminderNext() =
                    context.getString(R.string.fragment_anime_reminder_next_episode)
        }

        return inflater.inflate(R.layout.fragment_anime, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        streams.layoutManager = LinearLayoutManager(context)
        streams.adapter = adapter

        header.onReminderClickListener = {
            reminderTask.execute(ReminderInput(id, it, language, false))
        }

        header.onFinishClickListener = {
            reminderTask.execute(ReminderInput(id, it, language, true))
        }

        header.onSwitchClickListener = {
            switchEpisode(it)
        }

        header.setUploader(null)
        header.setTranslatorGroup(null)
        header.setDate(null)
    }

    override fun present(data: StreamInfo) {
        entryInfo = data.entryInfo

        if (data.streams.isEmpty()) {
            showError(getString(R.string.error_no_data_anime), null)
        } else {
            header.setEpisodeInfo(entryInfo.totalEpisodes!!, episode)
            streamAdapter.replace(data.streams)
            adapter.setHeader(header)
        }
    }

    override fun handleError(exception: Exception) {
        if (exception is ZippedTask.PartialException) {
            if (exception.data is EntryInfo) {
                entryInfo = exception.data

                header.setEpisodeInfo(entryInfo.totalEpisodes!!, episode)
                adapter.setHeader(header)
            }

            if (exception.original is ProxerException &&
                    exception.original.proxerErrorCode == ProxerException.ANIME_UNKNOWN_EPISODE) {
                showError(getString(R.string.fragment_anime_not_available), null)

                contentContainer.visibility = View.VISIBLE
                errorContainer.post {
                    errorContainer.y = ((root.height - header.height) / 2f + header.height) +
                            (errorText.layoutParams as ViewGroup.MarginLayoutParams).topMargin -
                            errorContainer.height
                }
            } else {
                super.handleError(exception.original)

                errorContainer.translationY = 0f
            }
        } else {
            super.handleError(exception)

            errorContainer.translationY = 0f
        }
    }

    override fun clear() {
        adapter.removeHeader()
        streamAdapter.clear()

        super.clear()
    }

    override fun onDestroyView() {
        adapter.removeHeader()
        streams.adapter = null
        streams.layoutManager = null

        super.onDestroyView()
    }

    override fun onDestroy() {
        reminderTask.destroy()
        streamResolverTask.destroy()
        streamAdapter.removeCallback()

        super.onDestroy()
    }

    override fun constructTask(): Task<Pair<AnimeInput, String>, StreamInfo> {
        return ZippedTask(
                ProxerLoadingTask({
                    StreamsRequest(it.id, it.episode, it.language).withIncludeProxerStreams(true)
                }),
                EntryInfoTask({ entryInfo }),
                zipFunction = ::StreamInfo,
                awaitSecondResult = true
        )
    }

    override fun constructInput(): Pair<AnimeInput, String> {
        return AnimeInput(id, episode, language) to id
    }

    private fun constructReminderTask(): Task<ReminderInput, Void?> {
        return ValidatingTask(ProxerLoadingTask({
            if (it.isFinished) {
                SetUserInfoRequest(it.id, ViewStateParameter.FINISHED)
            } else {
                SetReminderRequest(it.id, it.episode, it.language, CategoryParameter.ANIME)
            }
        }), { Validators.validateLogin() }, reminderSuccess, reminderException)
    }

    private fun constructStreamResolverTask(): Task<StreamResolverInput, StreamResolutionResult> {
        return ValidatingTask(StreamedTask(
                InputEchoTask(ProxerLoadingTask<StreamResolverInput, String>({ LinkRequest(it.id) })),
                StreamResolutionTask()
        ), { Validators.validateResolverExists(it.name) },
                streamResolverSuccess, streamResolverException).onStart {
            setRefreshing(true)
        }.onFinish {
            updateRefreshing()
        }
    }

    private fun switchEpisode(newEpisode: Int) {
        episode = newEpisode

        reset()
    }

    class AnimeInput(val id: String, val episode: Int, val language: String)
    class ReminderInput(val id: String, val episode: Int, val language: String,
                        val isFinished: Boolean)

    class StreamResolverInput(val name: String, val id: String)

    class StreamInfo(val streams: Array<Stream>, val entryInfo: EntryInfo)
}