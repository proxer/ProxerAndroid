package me.proxer.app.anime

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.enums.CalendarDay
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class CalendarFragment : BaseContentFragment<Map<CalendarDay, List<CalendarEntry>>>() {

    companion object {
        fun newInstance() = CalendarFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { CalendarViewModelProvider.get(this) }

    private var adapter by Delegates.notNull<CalendarAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView by bindView<RecyclerView>(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = CalendarAdapter()

        adapter.clickSubject
                .autoDispose(this)
                .subscribe { (view, item) ->
                    MediaActivity.navigateTo(safeActivity, item.entryId, item.name, null,
                            if (view.drawable != null) view else null)
                }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
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

    override fun showData(data: Map<CalendarDay, List<CalendarEntry>>) {
        super.showData(data)

        adapter.swapDataAndNotifyWithDiffing(data.toList())
    }

    override fun hideData() {
        adapter.swapDataAndNotifyWithDiffing(emptyList())

        super.hideData()
    }
}
