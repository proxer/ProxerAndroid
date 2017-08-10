package me.proxer.app.manga

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Build
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import android.support.design.widget.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import io.reactivex.Observable
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter.ContainerPositionResolver
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.snackbar
import me.proxer.app.view.MediaControlView
import me.proxer.app.view.MediaControlView.SimpleTranslatorGroup
import me.proxer.app.view.MediaControlView.Uploader
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.enums.Language
import org.jetbrains.anko.bundleOf

/**
 * @author Ruben Gees
 */
class MangaFragment : BaseContentFragment<MangaChapterInfo>() {

    companion object {
        fun newInstance() = MangaFragment().apply {
            arguments = bundleOf()
        }
    }

    override val hostingActivity: MangaActivity
        get() = activity as MangaActivity

    override val viewModel: MangaViewModel by lazy {
        ViewModelProviders.of(this).get(MangaViewModel::class.java).apply {
            entryId = this@MangaFragment.id
            language = this@MangaFragment.language
        }
    }

    private val id: String
        get() = hostingActivity.id

    private var episode: Int
        get() = hostingActivity.episode
        set(value) {
            hostingActivity.episode = value

            viewModel.setEpisode(value)
        }

    private val language: Language
        get() = hostingActivity.language

    private var chapterTitle: String?
        get() = hostingActivity.chapterTitle
        set(value) {
            hostingActivity.chapterTitle = value
        }

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

    private val mediaControlTextResolver = object : MediaControlView.TextResourceResolver {
        override fun next() = context.getString(R.string.fragment_manga_next_chapter)
        override fun previous() = context.getString(R.string.fragment_manga_previous_chapter)
        override fun bookmarkThis() = context.getString(R.string.fragment_manga_bookmark_this_chapter)
        override fun bookmarkNext() = context.getString(R.string.fragment_manga_bookmark_next_chapter)
    }

    private lateinit var innerAdapter: MangaAdapter
    private lateinit var adapter: EasyHeaderFooterAdapter

    private lateinit var header: MediaControlView
    private lateinit var footer: MediaControlView

    private val androidRoot by lazy { activity.findViewById<ViewGroup>(android.R.id.content) }
    private val toolbar by lazy { activity.findViewById<Toolbar>(R.id.toolbar) }
    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = MangaAdapter()
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        viewModel.setEpisode(episode, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val horizontalMargin = DeviceUtils.getHorizontalMargin(context, true)
        val verticalMargin = DeviceUtils.getVerticalMargin(context, true)

        header = (inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView).apply {
            textResolver = mediaControlTextResolver

            (layoutParams as ViewGroup.MarginLayoutParams).setMargins(horizontalMargin, verticalMargin,
                    horizontalMargin, verticalMargin)
        }

        footer = (inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView).apply {
            textResolver = mediaControlTextResolver

            (layoutParams as ViewGroup.MarginLayoutParams).setMargins(horizontalMargin, verticalMargin,
                    horizontalMargin, verticalMargin)
        }

        Observable.merge(header.uploaderClickSubject, footer.uploaderClickSubject)
                .bindToLifecycle(this@MangaFragment)
                .subscribe { ProfileActivity.navigateTo(activity, it.id, it.name) }

        Observable.merge(header.translatorGroupClickSubject, footer.translatorGroupClickSubject)
                .bindToLifecycle(this@MangaFragment)
                .subscribe { TranslatorGroupActivity.navigateTo(activity, it.id, it.name) }

        Observable.merge(header.episodeSwitchSubject, footer.episodeSwitchSubject)
                .bindToLifecycle(this@MangaFragment)
                .subscribe { episode = it }

        Observable.merge(header.bookmarkSetSubject, footer.bookmarkSetSubject)
                .bindToLifecycle(this@MangaFragment)
                .subscribe { viewModel.bookmark(it) }

        Observable.merge(header.finishClickSubject, footer.finishClickSubject)
                .bindToLifecycle(this@MangaFragment)
                .subscribe { viewModel.markAsFinished() }

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

        innerAdapter.positionResolver = ContainerPositionResolver(adapter)

        viewModel.bookmarkData.observe(this, Observer {
            it?.let {
                snackbar(androidRoot, R.string.fragment_set_user_info_success)
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

    override fun onDestroyView() {
        recyclerView.adapter = null
        recyclerView.layoutManager = null

        super.onDestroyView()
    }

    override fun showData(data: MangaChapterInfo) {
        super.showData(data)

        (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
            scrollFlags = SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS
        }

        chapterTitle = data.chapter.title
        episodeAmount = data.episodeAmount
        name = data.name

        header.setEpisodeInfo(data.episodeAmount, episode)
        header.setDateTime(data.chapter.date.convertToDateTime())
        header.setUploader(Uploader(data.chapter.uploaderId, data.chapter.uploaderName))

        footer.setEpisodeInfo(data.episodeAmount, episode)

        data.chapter.scanGroupId?.let { id ->
            data.chapter.scanGroupName?.let { name ->
                header.setTranslatorGroup(SimpleTranslatorGroup(id, name))
            }
        }

        adapter.header = header
        adapter.footer = footer

        innerAdapter.server = data.chapter.server
        innerAdapter.entryId = data.chapter.entryId
        innerAdapter.id = data.chapter.id
        innerAdapter.isLocal = data.isLocal

        innerAdapter.swapData(data.chapter.pages)

        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
    }

    override fun hideData() {
        super.hideData()

        (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
            scrollFlags = SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS
        }

        innerAdapter.clear()
        innerAdapter.notifyDataSetChanged()

        adapter.header = null
        adapter.footer = null
    }

    override fun showError(action: ErrorUtils.ErrorAction) {
        super.showError(action)

        chapterTitle = null

        action.partialData?.let {
            if (it is EntryCore) {
                episodeAmount = it.episodeAmount
                name = it.name

                header.setEpisodeInfo(it.episodeAmount, episode)
                header.setTranslatorGroup(null)
                header.setUploader(null)
                header.setDateTime(null)

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
