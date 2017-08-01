package me.proxer.app.media.episode

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.manga.MangaActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.enums.Category
import org.jetbrains.anko.bundleOf

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

    override val viewModel: EpisodeViewModel by lazy {
        ViewModelProviders.of(this).get(EpisodeViewModel::class.java).apply { entryId = id }
    }

    override val hostingActivity: MediaActivity
        get() = activity as MediaActivity

    private val id: String
        get() = hostingActivity.id

    private val name: String?
        get() = hostingActivity.name

    private val category: Category?
        get() = hostingActivity.category

    private lateinit var adapter: EpisodeAdapter

    private val recyclerView: RecyclerView by bindView(R.id.list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = EpisodeAdapter(savedInstanceState, id, GlideApp.with(this))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_episodes, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.languageClickSubject
                .bindToLifecycle(this)
                .subscribe { (language, episode) ->
                    when (episode.category) {
                        Category.ANIME -> {
//                            AnimeActivity.navigateTo(activity, id, episode.number, language.toAnimeLanguage(), name,
//                                    episode.episodeAmount)
                        }
                        Category.MANGA -> {
                            MangaActivity.navigateTo(activity, id, episode.number, language.toGeneralLanguage(),
                                    episode.title, name, episode.episodeAmount)
                        }
                    }
                }

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

        adapter.swapData(data)
        adapter.notifyItemRangeInserted(0, data.size)

        if (adapter.isEmpty()) {
            showError(ErrorUtils.ErrorAction(when (category) {
                Category.ANIME, null -> R.string.error_no_data_episodes
                Category.MANGA -> R.string.error_no_data_chapters
            }, ErrorUtils.ErrorAction.ACTION_MESSAGE_HIDE))
        }
    }
}
