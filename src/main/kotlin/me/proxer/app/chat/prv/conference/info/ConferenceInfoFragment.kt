package me.proxer.app.chat.prv.conference.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.messenger.ConferenceInfo
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ConferenceInfoFragment : BaseContentFragment<ConferenceInfo>() {

    companion object {
        fun newInstance() = ConferenceInfoFragment().apply {
            this.arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { ConferenceInfoViewModelProvider.get(this, id.toString()) }

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_conference_info, container, false)
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

        val dateTime = data.firstMessageTime.convertToDateTime()
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
