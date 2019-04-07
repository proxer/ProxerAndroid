package me.proxer.app.anime.schedule

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.enums.CalendarDay
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ScheduleFragment : BaseContentFragment<Map<CalendarDay, List<CalendarEntry>>>(R.layout.fragment_schedule) {

    companion object {
        fun newInstance() = ScheduleFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<ScheduleViewModel>()

    private var adapter by Delegates.notNull<ScheduleAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView by bindView<RecyclerView>(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ScheduleAdapter()

        adapter.clickSubject
            .autoDisposable(this.scope())
            .subscribe { (view, item) ->
                MediaActivity.navigateTo(requireActivity(), item.entryId, item.name, null, view)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.enableFastScroll()
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
