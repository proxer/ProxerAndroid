package com.proxerme.app.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.SQLException
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import butterknife.bindView
import com.klinker.android.link_builder.Link
import com.proxerme.app.R
import com.proxerme.app.activity.ChatActivity
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.ConferenceAdapter
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.event.ConferencesEvent
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.module.LoginModule
import com.proxerme.app.service.ChatService
import com.proxerme.app.util.Utils
import com.proxerme.app.util.listener.EndlessRecyclerOnScrollListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.util.concurrent.Future

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class ConferencesFragment : MainFragment() {

    companion object {
        fun newInstance(): ConferencesFragment {
            return ConferencesFragment()
        }
    }

    lateinit var layoutManager: StaggeredGridLayoutManager
    lateinit private var adapter: ConferenceAdapter

    private var refreshTask: Future<Unit>? = null

    private val loginModule = LoginModule(object : LoginModule.LoginModuleCallback {
        override val activity: AppCompatActivity
            get() = this@ConferencesFragment.activity as AppCompatActivity

        override fun showError(message: String, buttonMessage: String?,
                               onButtonClickListener: View.OnClickListener?) {
            this@ConferencesFragment.showError(message, buttonMessage, onButtonClickListener)
        }

        override fun load(showProgress: Boolean) {
            hideError()
            refresh()
        }
    })

    override val section: SectionManager.Section = SectionManager.Section.CONFERENCES

    private val list: RecyclerView by bindView(R.id.list)
    private val errorContainer: ViewGroup by bindView(R.id.errorContainer)
    private val errorText: TextView by bindView(R.id.errorText)
    private val errorButton: Button by bindView(R.id.errorButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.cancelNotification(context, NotificationHelper.CHAT_NOTIFICATION)

        adapter = ConferenceAdapter()
        adapter.callback = object : ConferenceAdapter.OnConferenceInteractionListener() {
            override fun onConferenceClick(v: View, conference: LocalConference) {
                ChatActivity.navigateTo(activity, conference)
            }

            override fun onConferenceImageClick(v: View, conference: LocalConference) {
                if (!conference.isGroup) {
                    UserActivity.navigateTo(activity, null, conference.topic,
                            conference.imageId)
                }
            }

            override fun onConferenceTopicClick(v: View, conference: LocalConference) {
                if (!conference.isGroup) {
                    UserActivity.navigateTo(activity, null, conference.topic,
                            conference.imageId)
                }
            }
        }

        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity),
                StaggeredGridLayoutManager.VERTICAL)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_paging_default, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = adapter
        list.addOnScrollListener(object : EndlessRecyclerOnScrollListener(layoutManager) {
            override fun onLoadMore() {
                if (!StorageHelper.conferenceListEndReached && !ChatService.isLoadingConferences &&
                        refreshTask?.isDone ?: true) {
                    ChatService.loadMoreConferences(context)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()

        loginModule.onResume()

        if (loginModule.canLoad()) {
            if (!ChatService.isSynchronizing) {
                ChatService.synchronize(context)
            }

            refresh()
        }
    }

    override fun onStart() {
        super.onStart()

        loginModule.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        loginModule.onStop()

        super.onStop()
    }

    override fun onDestroy() {
        refreshTask?.cancel(true)

        super.onDestroy()
    }

    @Subscribe
    fun onConferencesChanged(@Suppress("UNUSED_PARAMETER") event: ConferencesEvent) {
        if (loginModule.canLoad()) {
            refresh()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoadMoreConferencesException(exception: ChatService.LoadMoreConferencesException) {
        if (!StorageHelper.conferenceListEndReached) {
            showError(exception.message!!)
        }
    }

    private fun showError(message: String, buttonMessage: String? = null,
                          onButtonClickListener: View.OnClickListener? = null) {
        errorContainer.visibility = View.VISIBLE
        errorText.text = Utils.buildClickableText(context, message, Link.OnClickListener { link ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link + "?device=mobile")))
            } catch (exception: ActivityNotFoundException) {
                context.toast(R.string.link_error_not_found)
            }
        })

        if (buttonMessage == null) {
            errorButton.text = getString(R.string.error_retry)
        } else {
            errorButton.text = buttonMessage
        }

        if (onButtonClickListener == null) {
            errorButton.setOnClickListener {
                refresh()
            }
        } else {
            errorButton.setOnClickListener(onButtonClickListener)
        }

        clear()
    }

    private fun hideError() {
        errorContainer.visibility = View.INVISIBLE
    }

    private fun clear() {
        adapter.clear()
    }

    private fun refresh() {
        refreshTask?.cancel(true)

        refreshTask = doAsync {
            try {
                val conferences = context.chatDatabase.getConferences()

                if (conferences.isEmpty()) {
                    if (!StorageHelper.conferenceListEndReached &&
                            !ChatService.isLoadingConferences) {
                        ChatService.loadMoreConferences(context)
                    } else {
                        //TODO show empty view
                    }
                } else {
                    uiThread {
                        hideError()

                        adapter.replace(conferences)
                    }
                }
            } catch(exception: SQLException) {
                uiThread {
                    showError("Eine Datenbankabfrage ist fehlgeschlagen. Versuche es erneut")
                }
            }
        }
    }
}