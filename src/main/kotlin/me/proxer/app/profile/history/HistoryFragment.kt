package me.proxer.app.profile.history

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.anime.AnimeActivity
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.manga.MangaActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.enums.Category
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class HistoryFragment : PagedContentFragment<LocalUserHistoryEntry>() {

    companion object {
        fun newInstance() = HistoryFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage = R.string.error_no_data_history
    override val isSwipeToRefreshEnabled = false

    override val viewModel by viewModel<HistoryViewModel> { parametersOf(userId, username) }

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(
            DeviceUtils.calculateSpanAmount(requireActivity()) + 1,
            StaggeredGridLayoutManager.VERTICAL
        )
    }

    override val hostingActivity: ProfileActivity
        get() = activity as ProfileActivity

    private val userId: String?
        get() = hostingActivity.userId

    private val username: String?
        get() = hostingActivity.username

    override var innerAdapter by Delegates.notNull<HistoryAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = HistoryAdapter()

        innerAdapter.clickSubject
            .autoDisposable(this.scope())
            .subscribe { (_, entry) ->
                when (entry.category) {
                    Category.ANIME -> AnimeActivity.navigateTo(
                        requireActivity(),
                        entry.entryId,
                        entry.episode,
                        entry.language.toAnimeLanguage(),
                        entry.name
                    )
                    Category.MANGA, Category.NOVEL -> MangaActivity.navigateTo(
                        requireActivity(),
                        entry.entryId,
                        entry.episode,
                        entry.language.toGeneralLanguage(),
                        null,
                        entry.name
                    )
                }
            }

        innerAdapter.longClickSubject
            .autoDisposable(this.scope())
            .subscribe { (view, entry) ->
                MediaActivity.navigateTo(requireActivity(), entry.entryId, entry.name, entry.category, view)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
    }
}
