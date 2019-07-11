package me.proxer.app.settings.status

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.enableFastScroll
import me.proxer.app.util.extension.unsafeLazy
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Ruben Gees
 */
class ServerStatusFragment : BaseContentFragment<List<ServerStatus>>(R.layout.fragment_server_status) {

    companion object {
        fun newInstance() = ServerStatusFragment().apply {
            arguments = bundleOf()
        }
    }

    override val isSwipeToRefreshEnabled = true

    override val viewModel by viewModel<ServerStatusViewModel>()

    private val layoutManger by unsafeLazy {
        GridLayoutManager(requireContext(), DeviceUtils.calculateSpanAmount(requireActivity()))
    }

    private val adapter = ServerStatusAdapter()

    private val overallStatus: TextView by bindView(R.id.overallStatus)
    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.enableFastScroll()
        recyclerView.layoutManager = layoutManger
        recyclerView.adapter = adapter
    }

    override fun showData(data: List<ServerStatus>) {
        super.showData(data)

        val allServersOnline = data.all { it.online }

        val overallStatusText = when (allServersOnline) {
            true -> getString(R.string.fragment_server_status_overall_online)
            false -> getString(R.string.fragment_server_status_overall_offline)
        }

        val overallStatusIcon = IconicsDrawable(requireContext())
            .icon(if (allServersOnline) CommunityMaterial.Icon.cmd_earth else CommunityMaterial.Icon.cmd_earth_off)
            .colorRes((if (allServersOnline) R.color.md_green_500 else R.color.md_red_500))
            .sizeDp(48)
            .paddingDp(12)

        overallStatus.text = overallStatusText
        overallStatus.setCompoundDrawablesWithIntrinsicBounds(overallStatusIcon, null, null, null)

        adapter.swapDataAndNotifyWithDiffing(data)
    }
}
