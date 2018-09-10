package me.proxer.app.anime

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.base.BaseAdapter.ContainerPositionResolver
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ui.view.MediaControlView
import me.proxer.app.ui.view.MediaControlView.SimpleEpisodeInfo
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.Utils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.addReferer
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.safeData
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.AnimeLanguage
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.bundleOf
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class AnimeFragment : BaseContentFragment<AnimeStreamInfo>() {

    companion object {
        fun newInstance() = AnimeFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<AnimeViewModel> { parametersOf(id, language, episode) }

    override val hostingActivity: AnimeActivity
        get() = activity as AnimeActivity

    private val id: String
        get() = hostingActivity.id

    private var episode: Int
        get() = hostingActivity.episode
        set(value) {
            hostingActivity.episode = value

            viewModel.episode = value
        }

    private val language: AnimeLanguage
        get() = hostingActivity.language

    private var name: String?
        get() = hostingActivity.name
        set(value) {
            hostingActivity.name = value
        }

    private var episodeAmount: Int?
        get() = hostingActivity.episodeAmount
        set(value) {
            hostingActivity.episodeAmount = value
        }

    private var innerAdapter by Delegates.notNull<AnimeAdapter>()
    private var adapter by Delegates.notNull<EasyHeaderFooterAdapter>()

    private var header by Delegates.notNull<MediaControlView>()

    private val areBookmarksAutomatic by unsafeLazy { preferenceHelper.areBookmarksAutomatic }

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = AnimeAdapter(savedInstanceState)
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        innerAdapter.positionResolver = ContainerPositionResolver(adapter)

        innerAdapter.uploaderClickSubject
            .autoDisposable(this.scope())
            .subscribe { ProfileActivity.navigateTo(requireActivity(), it.uploaderId, it.uploaderName) }

        innerAdapter.translatorGroupClickSubject
            .autoDisposable(this.scope())
            .subscribe {
                it.translatorGroupId?.let { id ->
                    it.translatorGroupName?.let { name ->
                        TranslatorGroupActivity.navigateTo(requireActivity(), id, name)
                    }
                }
            }

        innerAdapter.playClickSubject
            .autoDisposable(this.scope())
            .subscribe { viewModel.resolve(it.hosterName, it.id) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        header = inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView

        header.textResolver = object : MediaControlView.TextResourceResolver {
            override fun next() = requireContext().getString(R.string.fragment_anime_next_episode)
            override fun previous() = requireContext().getString(R.string.fragment_anime_previous_episode)
            override fun bookmarkThis() = requireContext().getString(R.string.fragment_anime_bookmark_this_episode)
            override fun bookmarkNext() = requireContext().getString(R.string.fragment_anime_bookmark_next_episode)
        }

        header.episodeSwitchSubject
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                if (areBookmarksAutomatic && it > episode && StorageHelper.isLoggedIn) {
                    viewModel.bookmark(it)
                }

                episode = it
            }

        header.bookmarkSetSubject
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.bookmark(it) }

        header.finishClickSubject
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.markAsFinished() }

        return inflater.inflate(R.layout.fragment_anime, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        viewModel.resolutionResult.observe(viewLifecycleOwner, Observer { result ->
            result?.let {
                if (it.intent.action == Intent.ACTION_VIEW) {
                    if (it.intent.type == "text/html") {
                        showPage(Utils.getAndFixUrl(it.intent.safeData.toString()))
                    } else {
                        requireContext().startActivity(it.intent.addReferer())
                    }
                } else {
                    multilineSnackbar(root, it.intent.getCharSequenceExtra(StreamResolutionResult.MESSAGE_EXTRA))
                        ?.apply {
                            view.applyRecursively { view ->
                                if (view is TextView && view !is Button) {
                                    view.movementMethod = LinkMovementMethod.getInstance()
                                }
                            }
                        }
                }
            }
        })

        viewModel.resolutionError.observe(viewLifecycleOwner, Observer { errorAction ->
            errorAction?.let {
                when (it) {
                    is AppRequiredErrorAction -> it.showDialog(hostingActivity)
                    else -> multilineSnackbar(
                        root, it.message, Snackbar.LENGTH_LONG, it.buttonMessage,
                        it.toClickListener(hostingActivity)
                    )
                }
            }
        })

        viewModel.userStateData.observe(viewLifecycleOwner, Observer {
            it?.let { _ ->
                snackbar(root, R.string.fragment_set_user_info_success)
            }
        })

        viewModel.userStateError.observe(viewLifecycleOwner, Observer {
            it?.let { _ ->
                multilineSnackbar(
                    root, getString(R.string.error_set_user_info, getString(it.message)),
                    Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(hostingActivity)
                )
            }
        })
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun hideData() {
        innerAdapter.swapDataAndNotifyWithDiffing(emptyList())
        adapter.header = null

        super.hideData()
    }

    override fun showData(data: AnimeStreamInfo) {
        super.showData(data)

        episodeAmount = data.episodeAmount
        name = data.name

        header.episodeInfo = SimpleEpisodeInfo(data.episodeAmount, episode)
        adapter.header = header

        innerAdapter.swapDataAndNotifyWithDiffing(data.streams)

        if (data.streams.isEmpty()) {
            showError(ErrorAction(R.string.error_no_data_anime, ACTION_MESSAGE_HIDE))
        }
    }

    override fun showError(action: ErrorAction) {
        super.showError(action)

        action.data[ErrorUtils.ENTRY_DATA_KEY].let {
            if (it is EntryCore) {
                episodeAmount = it.episodeAmount
                name = it.name

                header.episodeInfo = SimpleEpisodeInfo(it.episodeAmount, episode)
                adapter.header = header
            }
        }

        if (adapter.header != null) {
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
}
