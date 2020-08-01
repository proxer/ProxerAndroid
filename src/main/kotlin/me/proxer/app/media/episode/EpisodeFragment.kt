package me.proxer.app.media.episode

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.anime.AnimeActivity
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.manga.MangaActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.media.MediaInfoViewModel
import me.proxer.app.media.episode.BookmarkLanguageDialog.Companion.LANGUAGE_RESULT
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.app.util.extension.getSafeString
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.snackbar
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.util.ProxerUtils
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class EpisodeFragment : BaseContentFragment<List<EpisodeRow>>(R.layout.fragment_episode) {

    companion object {
        private const val LANGUAGES_EXTRA = "languages"

        fun newInstance() = EpisodeFragment().apply {
            arguments = bundleOf()
        }
    }

    override val isSwipeToRefreshEnabled = false

    override val viewModel by viewModel<EpisodeViewModel> { parametersOf(id) }
    private val mediaInfoViewModel by sharedViewModel<MediaInfoViewModel>()

    override val hostingActivity: MediaActivity
        get() = activity as MediaActivity

    private val id: String
        get() = hostingActivity.id

    private val name: String?
        get() = hostingActivity.name

    private val category: Category?
        get() = hostingActivity.category

    private var languages: Set<MediaLanguage>?
        get() = requireArguments().getStringArrayList(LANGUAGES_EXTRA)
            ?.map { ProxerUtils.toSafeApiEnum<MediaLanguage>(it) }
            ?.toSet()
        set(value) {
            val stringLanguages = value
                ?.let { enums -> enums.map { ProxerUtils.getSafeApiEnumName(it) } }
                ?: emptyList()

            requireArguments().putStringArrayList(LANGUAGES_EXTRA, ArrayList(stringLanguages))
        }

    private val layoutManager by lazy { LinearLayoutManager(context) }
    private var adapter by Delegates.notNull<EpisodeAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)
    private val scrollToBottom: FloatingActionButton by bindView(R.id.scrollToBottom)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = EpisodeAdapter(savedInstanceState)

        adapter.languageClickSubject
            .autoDisposable(this.scope())
            .subscribe { (language, episode) ->
                when (episode.category) {
                    Category.ANIME -> AnimeActivity.navigateTo(
                        requireActivity(), id, episode.number,
                        language.toAnimeLanguage(), name, episode.episodeAmount
                    )
                    Category.MANGA, Category.NOVEL -> MangaActivity.navigateTo(
                        requireActivity(), id, episode.number,
                        language.toGeneralLanguage(), episode.title, name, episode.episodeAmount
                    )
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.enableFastScroll()
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        scrollToBottom.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32, colorAttr = R.attr.colorOnSurface)

        recyclerView.scrollEvents()
            .skip(1)
            .debounce(10, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { updateScrollToBottomVisibility() }

        hostingActivity.headerHeightChanges()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe { scrollToBottom.translationY = it }

        scrollToBottom.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                val indexBasedUserProgress = viewModel.data.value?.firstOrNull()?.userProgress?.minus(1) ?: 0
                val currentPosition = layoutManager.findLastVisibleItemPosition()

                val targetPosition = when (currentPosition >= indexBasedUserProgress) {
                    true -> if (adapter.itemCount == 0) 0 else adapter.itemCount - 1
                    false -> indexBasedUserProgress
                }

                hostingActivity.collapse()
                recyclerView.stopScroll()
                layoutManager.scrollToPositionWithOffset(targetPosition, 0)
            }

        mediaInfoViewModel.data.observe(
            viewLifecycleOwner,
            Observer {
                if (it != null) {
                    languages = it.languages

                    updateBookmarkErrorButton()
                }
            }
        )

        viewModel.bookmarkData.observe(
            viewLifecycleOwner,
            Observer {
                it?.let {
                    hostingActivity.snackbar(R.string.fragment_set_user_info_success)
                }
            }
        )

        viewModel.bookmarkError.observe(
            viewLifecycleOwner,
            Observer {
                it?.let {
                    hostingActivity.multilineSnackbar(
                        getString(R.string.error_set_user_info, getString(it.message)),
                        Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(hostingActivity)
                    )
                }
            }
        )

        storageHelper.isLoggedInObservable
            .autoDisposable(this.scope())
            .subscribe { updateBookmarkErrorButton() }

        setFragmentResultListener(LANGUAGE_RESULT) { _, bundle ->
            val language = ProxerUtils.toSafeApiEnum<MediaLanguage>(bundle.getSafeString(LANGUAGE_RESULT))
            val safeCategory = category

            if (safeCategory != null) {
                viewModel.bookmark(1, language, safeCategory)
            }
        }
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun showData(data: List<EpisodeRow>) {
        super.showData(data)

        adapter.swapDataAndNotifyWithDiffing(data)

        if (adapter.isEmpty()) {
            showError(
                ErrorAction(
                    when (category) {
                        Category.ANIME, null -> R.string.error_no_data_episodes
                        Category.MANGA, Category.NOVEL -> R.string.error_no_data_chapters
                    },
                    R.string.fragment_media_info_bookmark,
                    ButtonAction.BOOKMARK
                )
            )
        }

        recyclerView.post {
            if (view != null) updateScrollToBottomVisibility(false)
        }
    }

    override fun showError(action: ErrorAction) {
        super.showError(action)

        if (action.buttonAction == ButtonAction.BOOKMARK) {
            updateBookmarkErrorButton()
        }
    }

    private fun updateBookmarkErrorButton() {
        if (errorButton.text == getString(R.string.fragment_media_info_bookmark)) {
            val safeLanguages = languages

            if (safeLanguages != null && category != null && storageHelper.isLoggedIn) {
                errorButton.isVisible = true

                errorButton.clicks()
                    .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                    .subscribe { BookmarkLanguageDialog.show(requireActivity(), safeLanguages) }
            } else {
                errorButton.isVisible = false
            }
        }
    }

    private fun updateScrollToBottomVisibility(animate: Boolean = true) {
        when (layoutManager.findLastVisibleItemPosition()) {
            adapter.itemCount - 1 -> if (animate) scrollToBottom.hide() else scrollToBottom.isVisible = false
            else -> if (animate) scrollToBottom.show() else scrollToBottom.isVisible = true
        }
    }
}
