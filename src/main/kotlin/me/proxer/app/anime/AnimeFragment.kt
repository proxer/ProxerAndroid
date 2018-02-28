package me.proxer.app.anime

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.base.BaseAdapter
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ui.view.MediaControlView
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.AnimeLanguage
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.bundleOf
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

    override val viewModel by unsafeLazy { AnimeViewModelProvider.get(this, id, language, episode) }

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

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = AnimeAdapter(savedInstanceState)
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        innerAdapter.positionResolver = BaseAdapter.ContainerPositionResolver(adapter)

        innerAdapter.uploaderClickSubject
                .autoDispose(this)
                .subscribe { ProfileActivity.navigateTo(requireActivity(), it.uploaderId, it.uploaderName) }

        innerAdapter.translatorGroupClickSubject
                .autoDispose(this)
                .subscribe {
                    it.translatorGroupId?.let { id ->
                        it.translatorGroupName?.let { name ->
                            TranslatorGroupActivity.navigateTo(requireActivity(), id, name)
                        }
                    }
                }

        innerAdapter.playClickSubject
                .autoDispose(this)
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
                .autoDispose(this)
                .subscribe { episode = it }

        header.bookmarkSetSubject
                .autoDispose(this)
                .subscribe { viewModel.bookmark(it) }

        header.finishClickSubject
                .autoDispose(this)
                .subscribe { viewModel.markAsFinished() }

        return inflater.inflate(R.layout.fragment_anime, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)

        viewModel.resolutionResult.observe(this, Observer {
            it?.let {
                if (it.intent.action == Intent.ACTION_VIEW) {
                    if (it.intent.type == "text/html") {
                        showPage(Utils.parseAndFixUrl(it.intent.data.toString()))
                    } else {
                        requireContext().startActivity(it.intent)
                    }
                } else {
                    multilineSnackbar(root, it.intent.getCharSequenceExtra(StreamResolutionResult.MESSAGE_EXTRA))
                            ?.apply {
                                view.applyRecursively {
                                    if (it is TextView && it !is Button) {
                                        it.movementMethod = LinkMovementMethod.getInstance()
                                    }
                                }
                            }
                }
            }
        })

        viewModel.resolutionError.observe(this, Observer {
            it?.let {
                when (it) {
                    is AppRequiredErrorAction -> it.showDialog(hostingActivity)
                    else -> multilineSnackbar(root, it.message, Snackbar.LENGTH_LONG, it.buttonMessage,
                            it.buttonAction?.toClickListener(hostingActivity))
                }
            }
        })

        viewModel.bookmarkData.observe(this, Observer {
            it?.let {
                snackbar(root, R.string.fragment_set_user_info_success)
            }
        })

        viewModel.bookmarkError.observe(this, Observer {
            it?.let {
                multilineSnackbar(root, getString(R.string.error_set_user_info, getString(it.message)),
                        Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction?.toClickListener(hostingActivity))
            }
        })

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
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

        header.setEpisodeInfo(data.episodeAmount, episode)
        adapter.header = header

        innerAdapter.swapDataAndNotifyWithDiffing(data.streams)

        if (data.streams.isEmpty()) {
            showError(ErrorAction(R.string.error_no_data_anime, ACTION_MESSAGE_HIDE))
        }
    }

    override fun showError(action: ErrorAction) {
        super.showError(action)

        action.partialData?.let {
            if (it is EntryCore) {
                episodeAmount = it.episodeAmount
                name = it.name

                header.setEpisodeInfo(it.episodeAmount, episode)
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
