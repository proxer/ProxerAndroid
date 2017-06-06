package me.proxer.app.fragment.chat

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.R
import me.proxer.app.activity.ProfileActivity
import me.proxer.app.adapter.chat.ConferenceParticipantAdapter
import me.proxer.app.adapter.chat.ConferenceParticipantAdapter.ConferenceParticipantAdapterCallback
import me.proxer.app.application.GlideApp
import me.proxer.app.application.MainApplication.Companion.api
import me.proxer.app.fragment.base.LoadingFragment
import me.proxer.app.task.asyncProxerTask
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.extension.bindView
import me.proxer.library.api.ProxerCall
import me.proxer.library.entitiy.messenger.ConferenceInfo
import me.proxer.library.entitiy.messenger.ConferenceParticipant
import okhttp3.HttpUrl
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.find
import org.threeten.bp.format.DateTimeFormatter

/**
 * @author Ruben Gees
 */
class ConferenceInfoFragment : LoadingFragment<ProxerCall<ConferenceInfo>, ConferenceInfo>() {

    companion object {
        fun newInstance(): ConferenceInfoFragment {
            return ConferenceInfoFragment().apply {
                this.arguments = bundleOf()
            }
        }
    }

    private val conferenceInfoActivity
        get() = activity as me.proxer.app.activity.ConferenceInfoActivity

    private val id: String
        get() = conferenceInfoActivity.conference.id

    private val adapter by lazy { ConferenceParticipantAdapter(GlideApp.with(this)) }

    private val time: TextView by bindView(R.id.time)
    private val list: RecyclerView by bindView(R.id.participantList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.callback = object : ConferenceParticipantAdapterCallback {
            override fun onParticipantClick(view: View, item: ConferenceParticipant) {
                val imageView = view.find<ImageView>(R.id.image)

                ProfileActivity.navigateTo(activity, item.id, item.username, item.image,
                        if (imageView.drawable != null && item.image.isNotBlank()) imageView else null)
            }

            override fun onStatusLinkClick(link: HttpUrl) {
                showPage(link)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_conference_info, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.isNestedScrollingEnabled = false
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter
    }

    override fun onDestroy() {
        adapter.leader = null
        adapter.destroy()

        super.onDestroy()
    }

    override fun onSuccess(result: ConferenceInfo) {
        val dateTime = TimeUtils.convertToDateTime(result.firstMessageTime)

        val creationDate = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(dateTime)
        val creationTime = DateTimeFormatter.ofPattern("HH:mm").format(dateTime)

        time.text = getString(R.string.fragment_conference_info_time, creationDate, creationTime)

        adapter.leader = result.leaderId
        adapter.replace(result.participants)

        super.onSuccess(result)
    }

    override fun constructTask() = TaskBuilder.asyncProxerTask<ConferenceInfo>().build()
    override fun constructInput() = api.messenger().conferenceInfo(id).build()
}
