package me.proxer.app.manga

import android.app.ActivityManager
import android.os.Build
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
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.rubensousa.gravitysnaphelper.GravityPagerSnapHelper
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
import com.google.android.material.appbar.AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.rubengees.easyheaderfooteradapter.EasyHeaderFooterAdapter
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
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
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.safeLayoutManager
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.Language
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MangaFragment : BaseContentFragment<MangaChapterInfo>() {

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
    private var isVertical by Delegates.notNull<Boolean>()

    private val mediaControlTextResolver = object : MediaControlView.TextResourceResolver {
        override fun next() = requireContext().getString(R.string.fragment_manga_next_chapter)
        override fun previous() = requireContext().getString(R.string.fragment_manga_previous_chapter)
        override fun bookmarkThis() = requireContext().getString(R.string.fragment_manga_bookmark_this_chapter)
        override fun bookmarkNext() = requireContext().getString(R.string.fragment_manga_bookmark_next_chapter)
    }

    private var innerAdapter by Delegates.notNull<MangaAdapter>()
    private var adapter by Delegates.notNull<EasyHeaderFooterAdapter>()

    private var header by Delegates.notNull<MediaControlView>()
    private var footer by Delegates.notNull<MediaControlView>()

    private var gravityPagerSnapHelper: GravityPagerSnapHelper? = null

    private val areBookmarksAutomatic by unsafeLazy { preferenceHelper.areBookmarksAutomatic }
    private val mediumAnimationTime by unsafeLazy { resources.getInteger(android.R.integer.config_mediumAnimTime) }

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val activityRoot by unsafeLazy { requireActivity().findViewById<ViewGroup>(R.id.root) }
    private val toolbar by unsafeLazy { requireActivity().findViewById<Toolbar>(R.id.toolbar) }
    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lastPosition = savedInstanceState?.getParcelable(LAST_POSITION_STATE)
        hasLowMemory = savedInstanceState?.getByte(LOW_MEMORY_STATE) == 1.toByte()

        isVertical = preferenceHelper.isVerticalReaderEnabled

        innerAdapter = MangaAdapter(savedInstanceState, isVertical)
        adapter = EasyHeaderFooterAdapter(innerAdapter)

        innerAdapter.positionResolver = ContainerPositionResolver(adapter)

        innerAdapter.clickSubject
            .autoDisposable(this.scope())
            .subscribeAndLogErrors { (view, coordinates, position) ->
                val (xCoordinate, yCoordinate) = coordinates
                val parentHeight = recyclerView.height
                val parentWidth = recyclerView.width
                val normalizedParentHeight = parentHeight - parentHeight / 8

                if (isVertical) {
                    if (view.height > normalizedParentHeight) {
                        if (yCoordinate < parentHeight / 3) {
                            recyclerView.smoothScrollBy(0, -normalizedParentHeight)
                        } else {
                            recyclerView.smoothScrollBy(0, normalizedParentHeight)
                        }
                    } else {
                        if (yCoordinate < parentHeight / 3) {
                            recyclerView.smoothScrollBy(0, -view.height)
                        } else {
                            recyclerView.smoothScrollBy(0, view.height)
                        }
                    }
                } else {
                    // Add one to the position to account for the header.
                    if (xCoordinate < parentWidth / 3) {
                        recyclerView.smoothScrollToPosition(position - 1 + 1)
                    } else {
                        recyclerView.smoothScrollToPosition(position + 1 + 1)
                    }
                }
            }

        innerAdapter.lowMemorySubject
            .autoDisposable(this.scope())
            .subscribe {
                if (!hasLowMemory) {
                    multilineSnackbar(activityRoot, R.string.fragment_manga_low_memory)

                    hasLowMemory = true
                }
            }

        viewModel.setEpisode(episode, false)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val horizontalMargin = DeviceUtils.getHorizontalMargin(requireContext(), true)
        val verticalMargin = DeviceUtils.getVerticalMargin(requireContext(), true)

        header = (inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView).apply {
            textResolver = mediaControlTextResolver

            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                setMargins(
                    horizontalMargin, verticalMargin,
                    horizontalMargin, verticalMargin
                )
            }
        }

        footer = (inflater.inflate(R.layout.layout_media_control, container, false) as MediaControlView).apply {
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

        return inflater.inflate(R.layout.fragment_manga, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)

        bindLayoutManager()

        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        viewModel.userStateData.observe(viewLifecycleOwner, Observer {
            it?.let { _ ->
                snackbar(activityRoot, R.string.fragment_set_user_info_success)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_manga, menu, true)

        toolbar.doOnLayout {
            toolbar.findViewById<View>(R.id.toggle_orientation).rotation = if (isVertical) 0.0f else 90.0f
        }

        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toggle_orientation -> {
                isVertical = !isVertical

                preferenceHelper.isVerticalReaderEnabled = isVertical

                bindOrientationOptionsItem()
                bindToolbar()
                bindHeaderAndFooterHeight()
                bindLayoutManager()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        gravityPagerSnapHelper?.attachToRecyclerView(null)
        gravityPagerSnapHelper = null

        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)

        outState.putParcelable(LAST_POSITION_STATE, recyclerView.safeLayoutManager.onSaveInstanceState())
        outState.putByte(LOW_MEMORY_STATE, if (hasLowMemory) 1 else 0)
    }

    override fun showData(data: MangaChapterInfo) {
        super.showData(data)

        hostingActivity.toggleFullscreen(true)
        bindToolbar()

        chapterTitle = data.chapter.title
        episodeAmount = data.episodeAmount
        name = data.name

        showHeaderAndFooter(data)

        innerAdapter.server = data.chapter.server
        innerAdapter.entryId = data.chapter.entryId
        innerAdapter.id = data.chapter.id

        data.chapter.pages?.let { pages ->
            innerAdapter.swapDataAndNotifyWithDiffing(pages)
        }

        lastPosition?.let {
            recyclerView.safeLayoutManager.onRestoreInstanceState(it)

            lastPosition = null
        }
    }

    override fun hideData() {
        bindToolbar()

        innerAdapter.swapDataAndNotifyWithDiffing(emptyList())
        adapter.header = null
        adapter.footer = null

        super.hideData()
    }

    override fun showError(action: ErrorUtils.ErrorAction) {
        super.showError(action)

        hostingActivity.toggleFullscreen(false)

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
                header.doOnLayout { _ ->
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

    private fun showHeaderAndFooter(data: MangaChapterInfo) {
        bindHeaderAndFooterHeight()

        val translatorGroup = data.chapter.scanGroupId?.let { id ->
            data.chapter.scanGroupName?.let { name ->
                SimpleTranslatorGroup(id, name)
            }
        }

        header.episodeInfo = SimpleEpisodeInfo(data.episodeAmount, episode)
        header.uploader = Uploader(data.chapter.uploaderId, data.chapter.uploaderName)
        header.dateTime = data.chapter.date.convertToDateTime()
        header.translatorGroup = translatorGroup

        footer.episodeInfo = SimpleEpisodeInfo(data.episodeAmount, episode)

        adapter.header = header
        adapter.footer = footer
    }

    private fun bindOrientationOptionsItem() {
        toolbar.findViewById<View>(R.id.toggle_orientation).animate()
            .rotation(if (isVertical) 0.0f else 90.0f)
            .setDuration(mediumAnimationTime.toLong())
            .start()
    }

    private fun bindLayoutManager() {
        val state = recyclerView.layoutManager?.onSaveInstanceState()

        gravityPagerSnapHelper?.attachToRecyclerView(null)
        gravityPagerSnapHelper = null

        if (isVertical) {
            recyclerView.layoutManager = object : LinearLayoutManager(context) {
                override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
                    val isLowRamDevice = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT ||
                        requireContext().getSystemService<ActivityManager>()?.isLowRamDevice ?: true

                    return when {
                        isLowRamDevice -> 0
                        else -> DeviceUtils.getScreenHeight(requireContext())
                    }
                }
            }
        } else {
            recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            gravityPagerSnapHelper = GravityPagerSnapHelper(Gravity.END).apply { attachToRecyclerView(recyclerView) }
        }

        innerAdapter.isVertical = isVertical

        recyclerView.safeLayoutManager.onRestoreInstanceState(state)
    }

    private fun bindToolbar() {
        toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
            scrollFlags = when (isVertical && viewModel.data.value != null && viewModel.error.value == null) {
                true -> SCROLL_FLAG_SCROLL or SCROLL_FLAG_ENTER_ALWAYS
                false -> 0
            }
        }
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
