package me.proxer.app.anime

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
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
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.base.BaseAdapter
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.app.view.MediaControlView
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.enums.AnimeLanguage
import okhttp3.HttpUrl
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class AnimeFragment : BaseContentFragment<AnimeStreamInfo>() {

    companion object {
        fun newInstance() = AnimeFragment().apply {
            arguments = bundleOf()
        }
    }

    private val animeActivity
        get() = activity as AnimeActivity

    override val viewModel: AnimeViewModel by unsafeLazy {
        ViewModelProviders.of(this).get(AnimeViewModel::class.java).apply {
            entryId = this@AnimeFragment.id
            language = this@AnimeFragment.language
        }
    }

    private val id: String
        get() = animeActivity.id

    private var episode: Int
        get() = animeActivity.episode
        set(value) {
            animeActivity.episode = value

            viewModel.setEpisode(value)
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

    private lateinit var innerAdapter: AnimeAdapter
    private lateinit var adapter: EasyHeaderFooterAdapter

    private lateinit var header: MediaControlView

    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = AnimeAdapter(savedInstanceState, GlideApp.with(this))
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        viewModel.setEpisode(episode, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        header = inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView

        header.textResolver = object : MediaControlView.TextResourceResolver {
            override fun next() = context.getString(R.string.fragment_anime_next_episode)
            override fun previous() = context.getString(R.string.fragment_anime_previous_episode)
            override fun bookmarkThis() = context.getString(R.string.fragment_anime_bookmark_this_episode)
            override fun bookmarkNext() = context.getString(R.string.fragment_anime_bookmark_next_episode)
        }

        header.episodeSwitchSubject
                .bindToLifecycle(this)
                .subscribe { episode = it }

        header.bookmarkSetSubject
                .bindToLifecycle(this)
                .subscribe { viewModel.bookmark(it) }

        header.finishClickSubject
                .bindToLifecycle(this)
                .subscribe { viewModel.markAsFinished() }

        return inflater.inflate(R.layout.fragment_anime, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.positionResolver = BaseAdapter.ContainerPositionResolver(adapter)

        innerAdapter.uploaderClickSubject
                .bindToLifecycle(this)
                .subscribe { ProfileActivity.navigateTo(activity, it.uploaderId, it.uploaderName) }

        innerAdapter.translatorGroupClickSubject
                .bindToLifecycle(this)
                .subscribe {
                    it.translatorGroupId?.let { id ->
                        it.translatorGroupName?.let { name ->
                            TranslatorGroupActivity.navigateTo(activity, id, name)
                        }
                    }
                }

        innerAdapter.playClickSubject
                .bindToLifecycle(this)
                .subscribe { viewModel.resolve(it.hosterName, it.id) }

        viewModel.resolutionResult.observe(this, Observer {
            it?.let {
                if (it.intent.action == Intent.ACTION_VIEW) {
                    if (it.intent.type == "text/html") {
                        showPage(HttpUrl.parse(it.intent.data.toString()) ?: throw NullPointerException())
                    } else {
                        context.startActivity(it.intent)
                    }
                } else {
                    multilineSnackbar(root, it.intent.getCharSequenceExtra(StreamResolutionResult.MESSAGE_EXTRA))
                            .apply {
                                this.view.applyRecursively {
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
                multilineSnackbar(root, getString(R.string.fragment_set_user_info_error, getString(it.message)),
                        Snackbar.LENGTH_LONG, it.buttonMessage, it.buttonAction?.toClickListener(hostingActivity))
            }
        })

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }

    override fun hideData() {
        super.hideData()

        innerAdapter.clear()
        innerAdapter.notifyDataSetChanged()

        adapter.header = null
    }

    override fun showData(data: AnimeStreamInfo) {
        super.showData(data)

        episodeAmount = data.episodeAmount
        name = data.name

        header.setEpisodeInfo(data.episodeAmount, episode)
        innerAdapter.swapData(data.streams)
        adapter.header = header

        if (data.streams.isEmpty()) {
            showError(ErrorAction(R.string.error_no_data_anime, ErrorAction.ACTION_MESSAGE_HIDE))
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
