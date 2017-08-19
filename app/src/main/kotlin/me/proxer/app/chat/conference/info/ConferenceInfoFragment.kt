package me.proxer.app.chat.conference.info

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.trello.rxlifecycle2.android.lifecycle.kotlin.bindToLifecycle
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entitiy.messenger.ConferenceInfo
import org.jetbrains.anko.bundleOf
import org.threeten.bp.format.DateTimeFormatter

/**
 * @author Ruben Gees
 */
class ConferenceInfoFragment : BaseContentFragment<ConferenceInfo>() {

    companion object {
        fun newInstance() = ConferenceInfoFragment().apply {
            this.arguments = bundleOf()
        }
    }

    override val viewModel: ConferenceInfoViewModel by unsafeLazy {
        ViewModelProviders.of(this).get(ConferenceInfoViewModel::class.java).apply {
            conferenceId = id.toString()
        }
    }

    override val hostingActivity: ConferenceInfoActivity
        get() = activity as ConferenceInfoActivity

    private val id: Long
        get() = hostingActivity.conference.id

    private lateinit var adapter: ConferenceParticipantAdapter

    private val time: TextView by bindView(R.id.time)
    private val list: RecyclerView by bindView(R.id.participantList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConferenceParticipantAdapter()

        adapter.participantClickSubject
                .bindToLifecycle(this)
                .subscribe { (view, item) ->
                    ProfileActivity.navigateTo(activity, item.id, item.username, item.image,
                            if (view.drawable != null && item.image.isNotBlank()) view else null)
                }

        adapter.statusLinkClickSubject
                .bindToLifecycle(this)
                .subscribe { showPage(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_conference_info, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        list.isNestedScrollingEnabled = false
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
    }

    override fun showData(data: ConferenceInfo) {
        super.showData(data)

        val dateTime = data.firstMessageTime.convertToDateTime()
        val creationDate = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(dateTime)
        val creationTime = DateTimeFormatter.ofPattern("HH:mm").format(dateTime)

        time.text = getString(R.string.fragment_conference_info_time, creationDate, creationTime)

        adapter.leaderId = data.leaderId
        adapter.swapDataAndNotifyInsertion(data.participants)
    }

    override fun hideData() {
        adapter.clearAndNotifyRemoval()

        super.hideData()
    }
}
