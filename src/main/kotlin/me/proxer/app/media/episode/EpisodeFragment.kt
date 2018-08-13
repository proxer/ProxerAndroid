package me.proxer.app.media.episode

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.support.v7.widget.scrollEvents
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.anime.AnimeActivity
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.manga.MangaActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category
import org.jetbrains.anko.bundleOf
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class EpisodeFragment : BaseContentFragment<List<EpisodeRow>>() {

    companion object {
        fun newInstance() = EpisodeFragment().apply {
            arguments = bundleOf()
        }
    }

    override val isSwipeToRefreshEnabled = false

    override val viewModel by unsafeLazy { EpisodeViewModelProvider.get(this, id) }

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
                    Category.ANIME -> AnimeActivity.navigateTo(requireActivity(), id, episode.number,
                        language.toAnimeLanguage(), name, episode.episodeAmount)
                    Category.MANGA -> MangaActivity.navigateTo(requireActivity(), id, episode.number,
                        language.toGeneralLanguage(), episode.title, name, episode.episodeAmount)
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_episode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        scrollToBottom.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32, colorRes = R.color.textColorPrimary)

        recyclerView.scrollEvents()
            .debounce(10, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                val currentPosition = layoutManager.findLastVisibleItemPosition()

                when (currentPosition) {
                    adapter.itemCount - 1 -> scrollToBottom.hide()
                    else -> scrollToBottom.show()
                }
            }

        scrollToBottom.clicks()
            .autoDisposable(viewLifecycleOwner.scope())
            .subscribe {
                val indexBasedUserProgress = viewModel.data.value?.firstOrNull()?.userProgress?.minus(1) ?: 0
                val currentPosition = layoutManager.findLastVisibleItemPosition()

                val targetPosition = when (currentPosition >= indexBasedUserProgress) {
                    true -> if (adapter.itemCount == 0) 0 else adapter.itemCount - 1
                    false -> indexBasedUserProgress
                }

                recyclerView.stopScroll()
                layoutManager.scrollToPositionWithOffset(targetPosition, 0)
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
            showError(ErrorAction(when (category) {
                Category.ANIME, null -> R.string.error_no_data_episodes
                Category.MANGA -> R.string.error_no_data_chapters
            }, ACTION_MESSAGE_HIDE))
        }
    }
}
