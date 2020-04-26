package me.proxer.app.manga

import android.os.Bundle
import android.os.Parcelable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.rubensousa.gravitysnaphelper.GravitySnapHelper
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter.ContainerPositionResolver
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.ui.view.MediaControlView
import me.proxer.app.ui.view.MediaControlView.SimpleEpisodeInfo
import me.proxer.app.ui.view.MediaControlView.SimpleTranslatorGroup
import me.proxer.app.ui.view.MediaControlView.Uploader
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.safeLayoutManager
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toLocalDateTimeBP
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.Language
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MangaFragment : BaseContentFragment<MangaChapterInfo>(R.layout.fragment_manga) {

    companion object {
        private const val LAST_POSITION_STATE = "fragment_manga_last_position"
        private const val LOW_MEMORY_STATE = "fragment_manga_low_memory"

        fun newInstance() = MangaFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<MangaViewModel> { parametersOf(id, language, episode) }

    override val hostingActivity: MangaActivity
        get() = activity as MangaActivity

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

    private var hasLowMemory = false
    private var lastPosition: Parcelable? = null

    private val mediaControlTextResolver = object : MediaControlView.TextResourceResolver {
        override fun next() = requireContext().getString(R.string.fragment_manga_next_chapter)
        override fun previous() = requireContext().getString(R.string.fragment_manga_previous_chapter)
        override fun bookmarkThis() = requireContext().getString(R.string.fragment_manga_bookmark_this_chapter)
        override fun bookmarkNext() = requireContext().getString(R.string.fragment_manga_bookmark_next_chapter)
    }

    private var preloader by Delegates.notNull<MangaPreloader>()
    private var innerAdapter by Delegates.notNull<MangaAdapter>()
    private var adapter by Delegates.notNull<EasyHeaderFooterAdapter>()

    private var header by Delegates.notNull<MediaControlView>()
    private var footer by Delegates.notNull<MediaControlView>()

    private var gravitySnapHelper: GravitySnapHelper? = null

    private var readerOrientation = preferenceHelper.mangaReaderOrientation
        set(value) {
            preferenceHelper.mangaReaderOrientation = value

            field = value
        }

    private val isVertical get() = readerOrientation == MangaReaderOrientation.VERTICAL
    private val areBookmarksAutomatic by unsafeLazy { preferenceHelper.areBookmarksAutomatic }
    private val mediumAnimationTime by unsafeLazy { resources.getInteger(android.R.integer.config_mediumAnimTime) }

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val toolbar by unsafeLazy { requireActivity().findViewById<Toolbar>(R.id.toolbar) }
    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lastPosition = savedInstanceState?.getParcelable(LAST_POSITION_STATE)
        hasLowMemory = savedInstanceState?.getByte(LOW_MEMORY_STATE) == 1.toByte()

        preloader = MangaPreloader()
        innerAdapter = MangaAdapter(isVertical)
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        innerAdapter.positionResolver = ContainerPositionResolver(adapter)

        innerAdapter.clickSubject
            .autoDisposable(this.scope())
            .subscribeAndLogErrors { (_, coordinates, position) ->
                val (xCoordinate, yCoordinate) = coordinates
                val parentHeight = recyclerView.height
                val parentWidth = recyclerView.width
                val normalizedParentHeight = parentHeight / 2

                val widthPart = parentWidth / 4
                val heightPart = parentHeight / 4

                // Add one to the position to account for the header.
                when (readerOrientation) {
                    MangaReaderOrientation.LEFT_TO_RIGHT -> when {
                        xCoordinate < widthPart -> recyclerView.smoothScrollToPosition(position - 1 + 1)
                        xCoordinate >= widthPart && xCoordinate < widthPart * 3 -> hostingActivity.toggleFullscreen()
                        else -> recyclerView.smoothScrollToPosition(position + 1 + 1)
                    }
                    MangaReaderOrientation.RIGHT_TO_LEFT -> when {
                        xCoordinate < widthPart -> recyclerView.smoothScrollToPosition(position + 1 + 1)
                        xCoordinate >= widthPart && xCoordinate < widthPart * 3 -> hostingActivity.toggleFullscreen()
                        else -> recyclerView.smoothScrollToPosition(position - 1 + 1)
                    }
                    MangaReaderOrientation.VERTICAL -> when {
                        yCoordinate < heightPart -> recyclerView.smoothScrollBy(0, -normalizedParentHeight)
                        yCoordinate >= heightPart && yCoordinate < heightPart * 3 -> hostingActivity.toggleFullscreen()
                        else -> recyclerView.smoothScrollBy(0, normalizedParentHeight)
                    }
                }
            }

        innerAdapter.lowMemorySubject
            .autoDisposable(this.scope())
            .subscribe {
                if (!hasLowMemory) {
                    hostingActivity.multilineSnackbar(R.string.fragment_manga_low_memory)

                    hasLowMemory = true
                }
            }

        viewModel.setEpisode(episode, false)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        header = inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView
        footer = inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initHeaderAndFooter()

        preloader.glide = GlideApp.with(this)
        innerAdapter.glide = GlideApp.with(this)

        bindLayoutManager()

        recyclerView.setItemViewCacheSize(1)
        recyclerView.setHasFixedSize(true)
        recyclerView.enableFastScroll()

        recyclerView.itemAnimator = null
        recyclerView.adapter = adapter

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

    override fun onStop() {
        (recyclerView.safeLayoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()?.let {
            if (it > 0 || storageHelper.getLastMangaPage(id, episode, language) != null) {
                storageHelper.putLastMangaPage(id, episode, language, it)
            }
        }

        super.onStop()
    }

    override fun onDestroyView() {
        gravitySnapHelper?.attachToRecyclerView(null)
        gravitySnapHelper = null

        recyclerView.layoutManager = null
        recyclerView.adapter = null

        preloader.glide = null

        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, requireContext(), R.menu.fragment_manga, menu, true)

        toolbar.doOnLayout {
            toolbar.findViewById<View>(R.id.toggle_orientation).rotation = when (readerOrientation) {
                MangaReaderOrientation.LEFT_TO_RIGHT -> 90f
                MangaReaderOrientation.RIGHT_TO_LEFT -> 270f
                MangaReaderOrientation.VERTICAL -> 0.0f
            }
        }

        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toggle_orientation -> {
                readerOrientation = when (readerOrientation) {
                    MangaReaderOrientation.LEFT_TO_RIGHT -> MangaReaderOrientation.RIGHT_TO_LEFT
                    MangaReaderOrientation.RIGHT_TO_LEFT -> MangaReaderOrientation.VERTICAL
                    MangaReaderOrientation.VERTICAL -> MangaReaderOrientation.LEFT_TO_RIGHT
                }

                bindOrientationOptionsItem()
                bindHeaderAndFooterHeight()
                bindLayoutManager()

                hostingActivity.multilineSnackbar(
                    when (readerOrientation) {
                        MangaReaderOrientation.LEFT_TO_RIGHT -> R.string.fragment_manga_left_to_right
                        MangaReaderOrientation.RIGHT_TO_LEFT -> R.string.fragment_manga_right_to_left
                        MangaReaderOrientation.VERTICAL -> R.string.fragment_manga_vertical
                    }
                )

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)

        outState.putParcelable(LAST_POSITION_STATE, recyclerView.safeLayoutManager.onSaveInstanceState())
        outState.putByte(LOW_MEMORY_STATE, if (hasLowMemory) 1 else 0)
    }

    override fun showData(data: MangaChapterInfo) {
        super.showData(data)

        hostingActivity.onContentShow()

        chapterTitle = data.chapter.title
        episodeAmount = data.episodeAmount
        name = data.name

        showHeaderAndFooter(data)

        preloader.preload(data.chapter)
        innerAdapter.setChapter(data.chapter)

        data.chapter.pages?.let { pages ->
            innerAdapter.swapDataAndNotifyWithDiffing(pages)
        }

        lastPosition.let { safeLastPosition ->
            if (safeLastPosition != null) {
                recyclerView.safeLayoutManager.onRestoreInstanceState(safeLastPosition)

                lastPosition = null
            } else {
                val lastPage = storageHelper.getLastMangaPage(id, episode, language)

                if (lastPage != null && lastPage > 0) {
                    (recyclerView.safeLayoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(lastPage, 0)
                }
            }
        }
    }

    override fun hideData() {
        preloader.cancel()
        innerAdapter.swapDataAndNotifyWithDiffing(emptyList())

        if (viewModel.error.value?.data?.get(ErrorUtils.ENTRY_DATA_KEY) !is EntryCore) {
            adapter.header = null
            adapter.footer = null

            super.hideData()
        }
    }

    override fun showError(action: ErrorUtils.ErrorAction) {
        super.showError(action)

        hostingActivity.onContentHide()

        action.data[ErrorUtils.CHAPTER_TITLE_DATA_KEY].let {
            chapterTitle = it as? String
        }

        action.data[ErrorUtils.ENTRY_DATA_KEY].let {
            if (it is EntryCore) {
                episodeAmount = it.episodeAmount
                name = it.name

                header.episodeInfo = SimpleEpisodeInfo(it.episodeAmount, episode)
                header.translatorGroup = null
                header.uploader = null
                header.dateTime = null

                adapter.header = header
            }
        }

        if (adapter.header != null) {
            bindHeaderAndFooterHeight()

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

    private fun initHeaderAndFooter() {
        val horizontalMargin = DeviceUtils.getHorizontalMargin(requireContext(), true)
        val verticalMargin = DeviceUtils.getVerticalMargin(requireContext(), true)

        header.apply {
            textResolver = mediaControlTextResolver

            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                setMargins(
                    horizontalMargin, verticalMargin,
                    horizontalMargin, verticalMargin
                )
            }
        }

        footer.apply {
            textResolver = mediaControlTextResolver

            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                setMargins(
                    horizontalMargin, verticalMargin,
                    horizontalMargin, verticalMargin
                )
            }
        }

        Observable.merge(header.uploaderClickSubject, footer.uploaderClickSubject)
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { ProfileActivity.navigateTo(requireActivity(), it.id, it.name) }

        Observable.merge(header.translatorGroupClickSubject, footer.translatorGroupClickSubject)
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { TranslatorGroupActivity.navigateTo(requireActivity(), it.id, it.name) }

        Observable.merge(header.episodeSwitchSubject, footer.episodeSwitchSubject)
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                if (areBookmarksAutomatic && it > episode && storageHelper.isLoggedIn) {
                    viewModel.bookmark(it)
                }

                episode = it
            }

        Observable.merge(header.bookmarkSetSubject, footer.bookmarkSetSubject)
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.bookmark(it) }

        Observable.merge(header.finishClickSubject, footer.finishClickSubject)
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { viewModel.markAsFinished() }
    }

    private fun showHeaderAndFooter(data: MangaChapterInfo) {
        bindHeaderAndFooterHeight()

        val translatorGroup = data.chapter.scanGroupId?.let { id ->
            data.chapter.scanGroupName?.let { name ->
                SimpleTranslatorGroup(id, name)
            }
        }

        header.episodeInfo = SimpleEpisodeInfo(data.episodeAmount, episode)
        header.uploader = Uploader(data.chapter.uploaderId, data.chapter.uploaderName)
        header.dateTime = data.chapter.date.toLocalDateTimeBP()
        header.translatorGroup = translatorGroup

        footer.episodeInfo = SimpleEpisodeInfo(data.episodeAmount, episode)

        adapter.header = header
        adapter.footer = footer
    }

    private fun bindOrientationOptionsItem() {
        val rotation = when (readerOrientation) {
            MangaReaderOrientation.LEFT_TO_RIGHT -> 90f
            MangaReaderOrientation.RIGHT_TO_LEFT -> 270f
            MangaReaderOrientation.VERTICAL -> 0.0f
        }

        toolbar.findViewById<View>(R.id.toggle_orientation).animate()
            .rotation(rotation)
            .setDuration(mediumAnimationTime.toLong())
            .start()
    }

    private fun bindLayoutManager() {
        val state = recyclerView.layoutManager?.onSaveInstanceState()

        gravitySnapHelper?.attachToRecyclerView(null)
        gravitySnapHelper = null

        recyclerView.recycledViewPool.clear()

        recyclerView.layoutManager = MangaLinearLayoutManger(requireContext(), readerOrientation)

        if (!isVertical) {
            gravitySnapHelper = GravitySnapHelper(Gravity.END)
                .apply { maxFlingSizeFraction = 1.0f }
                .apply { scrollMsPerInch = 50f }
                .apply { attachToRecyclerView(recyclerView) }
        }

        innerAdapter.isVertical = isVertical

        recyclerView.safeLayoutManager.onRestoreInstanceState(state)
    }

    private fun bindHeaderAndFooterHeight() {
        if (isVertical || viewModel.error.value != null) {
            header.layoutParams.height = WRAP_CONTENT
            footer.layoutParams.height = WRAP_CONTENT
        } else {
            header.layoutParams.height = MATCH_PARENT
            footer.layoutParams.height = MATCH_PARENT
        }
    }
}
