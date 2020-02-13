package me.proxer.app.media.episode

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.sizeDp
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
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.extension.colorAttr
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.enums.Category
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

        scrollToBottom.setIconicsImage(CommunityMaterial.Icon3.cmd_chevron_down, 32, colorAttr = R.attr.colorOnSurface)

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

        mediaInfoViewModel.userInfoData.observe(viewLifecycleOwner, Observer {
            if (it?.isSubscribed == true) {
                val icon = IconicsDrawable(requireContext(), CommunityMaterial.Icon3.cmd_check)
                    .colorAttr(requireContext(), R.attr.colorOnPrimary)
                    .sizeDp(18)

                errorButton.setCompoundDrawables(null, null, icon, null)
            } else {
                errorButton.setCompoundDrawables(null, null, null, null)
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
                    R.string.fragment_media_info_subscribe,
                    ButtonAction.SUBSCRIBE
                )
            )
        }

        recyclerView.post {
            if (view != null) updateScrollToBottomVisibility(false)
        }
    }

    override fun showError(action: ErrorAction) {
        super.showError(action)

        if (action.buttonAction == ButtonAction.SUBSCRIBE) {
            errorButton.clicks()
                .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                .subscribe { mediaInfoViewModel.toggleSubscription() }
        }
    }

    private fun updateScrollToBottomVisibility(animate: Boolean = true) {
        when (layoutManager.findLastVisibleItemPosition()) {
            adapter.itemCount - 1 -> if (animate) scrollToBottom.hide() else scrollToBottom.isVisible = false
            else -> if (animate) scrollToBottom.show() else scrollToBottom.isVisible = true
        }
    }
}
