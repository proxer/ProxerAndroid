package me.proxer.app.settings.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Ruben Gees
 */
class ServerStatusFragment : BaseContentFragment<List<ServerStatus>>() {

    companion object {
        fun newInstance() = ServerStatusFragment().apply {
            arguments = bundleOf()
        }
    }

    override val isSwipeToRefreshEnabled = true

    override val viewModel by viewModel<ServerStatusViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_server_status, container, false)
    }
}
