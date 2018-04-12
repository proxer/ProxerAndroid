package me.proxer.app.ucp.history

import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.anime.AnimeActivity
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.manga.MangaActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.ucp.UcpHistoryEntry
import me.proxer.library.enums.Category
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class UcpHistoryFragment : PagedContentFragment<UcpHistoryEntry>() {

    companion object {
        fun newInstance() = UcpHistoryFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage = R.string.error_no_data_history
    override val isSwipeToRefreshEnabled = false

    override val viewModel by unsafeLazy { UcpHistoryViewModelProvider.get(this) }

    override val layoutManager by lazy {
        StaggeredGridLayoutManager(DeviceUtils.calculateSpanAmount(requireActivity()) + 1,
            StaggeredGridLayoutManager.VERTICAL)
    }

    override var innerAdapter by Delegates.notNull<UcpHistoryAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = UcpHistoryAdapter()

        innerAdapter.clickSubject
            .autoDispose(this)
            .subscribe { (_, entry) ->
                when (entry.category) {
                    Category.ANIME -> AnimeActivity.navigateTo(requireActivity(), entry.entryId, entry.episode,
                        entry.language.toAnimeLanguage(), entry.name)
                    Category.MANGA -> MangaActivity.navigateTo(requireActivity(), entry.entryId, entry.episode,
                        entry.language.toGeneralLanguage(), null, entry.name)
                }
            }

        innerAdapter.longClickSubject
            .autoDispose(this)
            .subscribe { (view, entry) ->
                MediaActivity.navigateTo(requireActivity(), entry.entryId, entry.name, entry.category,
                    if (view.drawable != null) view else null)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerAdapter.glide = GlideApp.with(this)
    }
}
