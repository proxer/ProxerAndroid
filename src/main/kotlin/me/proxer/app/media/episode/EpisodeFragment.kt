package me.proxer.app.media.episode

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.anime.AnimeActivity
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.manga.MangaActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category
import org.jetbrains.anko.bundleOf
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

    private var adapter by Delegates.notNull<EpisodeAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = EpisodeAdapter(savedInstanceState, id)

        adapter.languageClickSubject
                .autoDispose(this)
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
