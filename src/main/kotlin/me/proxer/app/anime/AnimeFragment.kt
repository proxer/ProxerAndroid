package me.proxer.app.anime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ContentView
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.auth.LoginDialog
import me.proxer.app.base.BaseAdapter.ContainerPositionResolver
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ui.view.MediaControlView
import me.proxer.app.ui.view.MediaControlView.SimpleEpisodeInfo
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.AnimeLanguage
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@ContentView(R.layout.fragment_anime)
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

        innerAdapter = AnimeAdapter(savedInstanceState, storageHelper)
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
            .subscribe { viewModel.resolve(it) }

        innerAdapter.loginClickSubject
            .autoDisposable(this.scope())
            .subscribe { LoginDialog.show(hostingActivity) }

        innerAdapter.linkClickSubject
            .autoDisposable(this.scope())
            .subscribe { showPage(it) }

        storageHelper.isLoggedInObservable
            .skip(1)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDisposable(this.scope())
            .subscribe {
                if (adapter.itemCount >= 1) {
                    adapter.notifyItemRangeChanged(1, innerAdapter.itemCount + 1)
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
                if (areBookmarksAutomatic && it > episode && storageHelper.isLoggedIn) {
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

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.enableFastScroll()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        viewModel.resolutionResult.observe(viewLifecycleOwner, Observer { result ->
            result?.let {
                when (result) {
                    is StreamResolutionResult.Video -> result.play(requireContext(), name, episode, true)
                    is StreamResolutionResult.Link -> result.show(this)
                    is StreamResolutionResult.App -> result.navigate(requireContext())
                    is StreamResolutionResult.Message -> throw IllegalArgumentException(
                        "ResolutionResult of type Message should be shown inline"
                    )
                }
            }
        })

        viewModel.resolutionError.observe(viewLifecycleOwner, Observer { errorAction ->
            errorAction?.let {
                when (it) {
                    is AppRequiredErrorAction -> it.showDialog(hostingActivity)
                    else -> hostingActivity.multilineSnackbar(
                        it.message, Snackbar.LENGTH_LONG, it.buttonMessage,
                        it.toClickListener(hostingActivity)
                    )
                }
            }
        })

        viewModel.userStateData.observe(viewLifecycleOwner, Observer {
            it?.let {
                hostingActivity.snackbar(R.string.fragment_set_user_info_success)
            }
        })

        viewModel.userStateError.observe(viewLifecycleOwner, Observer {
            it?.let {
                hostingActivity.multilineSnackbar(
                    getString(R.string.error_set_user_info, getString(it.message)),
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

    override fun hideData() {
        innerAdapter.swapDataAndNotifyWithDiffing(emptyList())

        if (viewModel.error.value?.data?.get(ErrorUtils.ENTRY_DATA_KEY) !is EntryCore) {
            adapter.header = null

            super.hideData()
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
            contentContainer.isVisible = true
            errorContainer.isInvisible = true

            errorInnerContainer.doOnLayout {
                header.doOnLayout {
                    val newCenter = root.height / 2f + header.height / 2f
                    val containerCenterCorrection = errorInnerContainer.height / 2f

                    errorInnerContainer.y = newCenter - containerCenterCorrection
                    errorContainer.isVisible = true
                }
            }
        } else {
            errorContainer.translationY = 0f
        }
    }

    override fun hideError() {
        super.hideError()

        if (viewModel.data.value == null) {
            adapter.header = null
        }
    }
}
