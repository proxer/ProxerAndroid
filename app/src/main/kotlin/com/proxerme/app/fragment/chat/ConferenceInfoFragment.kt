package com.proxerme.app.fragment.chat

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.proxerme.app.R
import com.proxerme.app.activity.ProfileActivity
import com.proxerme.app.adapter.chat.ConferenceParticipantAdapter
import com.proxerme.app.adapter.chat.ConferenceParticipantAdapter.ConferenceParticipantAdapterCallback
import com.proxerme.app.fragment.framework.SingleLoadingFragment
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.task.ProxerLoadingTask
import com.proxerme.app.task.framework.Task
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.messenger.entity.ConferenceInfoContainer
import com.proxerme.library.connection.messenger.entity.ConferenceInfoUser
import com.proxerme.library.connection.messenger.request.ConferenceInfoRequest
import okhttp3.HttpUrl
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ConferenceInfoFragment : SingleLoadingFragment<String, ConferenceInfoContainer>() {

    companion object {
        private const val CONFERENCE_ID_ARGUMENT = "conference_id"

        fun newInstance(conferenceId: String): ConferenceInfoFragment {
            return ConferenceInfoFragment().apply {
                this.arguments = Bundle().apply {
                    this.putString(CONFERENCE_ID_ARGUMENT, conferenceId)
                }
            }
        }
    }

    override val section = SectionManager.Section.CONFERENCE_INFO

    private val conferenceId: String
        get() = arguments.getString(CONFERENCE_ID_ARGUMENT)

    private lateinit var adapter: ConferenceParticipantAdapter

    private val time: TextView by bindView(R.id.time)
    private val list: RecyclerView by bindView(R.id.participantList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConferenceParticipantAdapter()
        adapter.callback = object : ConferenceParticipantAdapterCallback() {
            override fun onItemClick(item: ConferenceInfoUser) {
                ProfileActivity.navigateTo(activity, item.id, item.username, item.imageId)
            }

            override fun onStatusLinkClick(link: HttpUrl) {
                showPage(link)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_conference_info, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.isNestedScrollingEnabled = false
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
    }

    override fun onDestroyView() {
        list.adapter = null
        list.layoutManager = null

        super.onDestroyView()
    }

    override fun clear() {
        adapter.leader = null
        adapter.clear()
    }

    override fun constructTask(): Task<String, ConferenceInfoContainer> {
        return ProxerLoadingTask(::ConferenceInfoRequest)
    }

    override fun constructInput(): String {
        return conferenceId
    }

    override fun present(data: ConferenceInfoContainer) {
        val dateTime = DateTime(data.conferenceInfo.firstMessageTime * 1000)
        val creationDate = dateTime.toString(DateTimeFormat.forPattern("dd.MM.yyyy"))
        val creationTime = dateTime.toString(DateTimeFormat.forPattern("HH:mm"))

        time.text = getString(R.string.fragment_conference_info_time, creationDate,
                creationTime)

        adapter.leader = data.conferenceInfo.leaderId
        adapter.replace(data.participants)
    }
}