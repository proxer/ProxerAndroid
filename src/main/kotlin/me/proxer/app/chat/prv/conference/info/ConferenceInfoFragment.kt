package me.proxer.app.chat.prv.conference.info

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.ContentView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.toDateTimeBP
import me.proxer.library.entity.messenger.ConferenceInfo
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@ContentView(R.layout.fragment_conference_info)
class ConferenceInfoFragment : BaseContentFragment<ConferenceInfo>() {

    companion object {
        fun newInstance() = ConferenceInfoFragment().apply {
            this.arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<ConferenceInfoViewModel> { parametersOf(id.toString()) }

    override val hostingActivity: ConferenceInfoActivity
        get() = activity as ConferenceInfoActivity

    private val id: Long
        get() = hostingActivity.conference.id

    private var adapter by Delegates.notNull<ConferenceParticipantAdapter>()

    private val time: TextView by bindView(R.id.time)
    private val list: RecyclerView by bindView(R.id.participantList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConferenceParticipantAdapter()

        adapter.participantClickSubject
            .autoDisposable(this.scope())
            .subscribe { (view, item) ->
                ProfileActivity.navigateTo(
                    requireActivity(), item.id, item.username, item.image,
                    if (view.drawable != null && item.image.isNotBlank()) view else null
                )
            }

        adapter.statusLinkClickSubject
            .autoDisposable(this.scope())
            .subscribe { showPage(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        list.isNestedScrollingEnabled = false
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
    }

    override fun showData(data: ConferenceInfo) {
        super.showData(data)

        val dateTime = data.firstMessageTime.toDateTimeBP()
        val creationDate = Utils.dateFormatter.format(dateTime)
        val creationTime = Utils.timeFormatter.format(dateTime)

        time.text = getString(R.string.fragment_conference_info_time, creationDate, creationTime)

        adapter.leaderId = data.leaderId
        adapter.swapDataAndNotifyWithDiffing(data.participants)
    }

    override fun hideData() {
        adapter.swapDataAndNotifyWithDiffing(emptyList())

        super.hideData()
    }
}
