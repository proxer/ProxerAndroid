package com.proxerme.app.fragment

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import android.widget.ImageView
import com.proxerme.app.activity.ChatActivity
import com.proxerme.app.activity.ImageDetailActivity
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.ConferenceAdapter
import com.proxerme.app.helper.NotificationHelper
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.module.LoginModule
import com.proxerme.app.module.PollingModule
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ProxerConnection
import com.proxerme.library.connection.experimental.chat.entity.Conference
import com.proxerme.library.connection.experimental.chat.request.ConferencesRequest
import com.proxerme.library.info.ProxerTag
import com.proxerme.library.info.ProxerUrlHolder
import com.proxerme.library.interfaces.ProxerErrorResult
import com.proxerme.library.interfaces.ProxerResult

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class ConferencesFragment : PagingFragment() {

    companion object {
        private const val POLLING_INTERVAL = 10000L

        fun newInstance(): ConferencesFragment {
            return ConferencesFragment()
        }
    }

    lateinit override var layoutManager: StaggeredGridLayoutManager
    lateinit private var adapter: ConferenceAdapter

    private val loginModule = LoginModule(object : LoginModule.LoginModuleCallback {
        override val activity: AppCompatActivity
            get() = this@ConferencesFragment.activity as AppCompatActivity

        override fun showError(message: String, buttonMessage: String?,
                               onButtonClickListener: View.OnClickListener?) {
            this@ConferencesFragment.showError(message, buttonMessage, onButtonClickListener)
        }

        override fun load(showProgress: Boolean) {
            this@ConferencesFragment.load(showProgress)
        }
    })

    override val firstPage = 1
    override val canLoad: Boolean
        get() = super.canLoad && loginModule.canLoad()
    override val loadAlways = true

    override val section: SectionManager.Section = SectionManager.Section.CONFERENCES

    private val pollingModule = PollingModule(object : PollingModule.PollingModuleCallback {
        override val isLoading: Boolean
            get() = this@ConferencesFragment.isLoading
        override val canLoad: Boolean
            get() = this@ConferencesFragment.canLoad && currentError == null

        override fun load(showProgress: Boolean) {
            this@ConferencesFragment.load(showProgress)
        }
    }, POLLING_INTERVAL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.cancelNotification(context, NotificationHelper.CHAT_NOTIFICATION)

        adapter = ConferenceAdapter(savedInstanceState)
        adapter.callback = object : ConferenceAdapter.OnConferenceInteractionListener() {
            override fun onConferenceClick(v: View, conference: Conference) {
                ChatActivity.navigateTo(activity, conference)
            }

            override fun onConferenceImageClick(v: View, conference: Conference) {
                ImageDetailActivity.navigateTo(activity, v as ImageView,
                        ProxerUrlHolder.getUserImageUrl(conference.imageId))
            }

            override fun onConferenceTopicClick(v: View, conference: Conference) {
                if (!conference.isConference) {
                    UserActivity.navigateTo(activity, null, conference.topic,
                            conference.imageId)
                }
            }
        }

        layoutManager = StaggeredGridLayoutManager(Utils.calculateSpanAmount(activity),
                StaggeredGridLayoutManager.VERTICAL)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.setHasFixedSize(true)
        list.layoutManager = layoutManager
        list.adapter = adapter
    }

    override fun onResume() {
        super.onResume()

        loginModule.onResume()
        pollingModule.onResume()
    }

    override fun onStart() {
        super.onStart()

        loginModule.onStart()
    }

    override fun onPause() {
        super.onPause()

        pollingModule.onPause()
    }

    override fun onStop() {
        loginModule.onStop()

        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun loadPage(number: Int) {
        ConferencesRequest(number).execute({ result ->
            if (result.item.isNotEmpty()) {
                adapter.addItems(result.item.asList())

                if (number == firstPage) {
                    StorageHelper.lastMessageTime = result.item.first().time
                    StorageHelper.newMessages = 0
                    StorageHelper.resetMessagesInterval()
                    NotificationHelper.retrieveChatLater(context)
                }
            }

            notifyPagedLoadFinishedSuccessful(number, result)
        }, { result ->
            notifyPagedLoadFinishedWithError(number, result)
        })
    }

    override fun clear() {
        adapter.clear()
    }

    override fun cancel() {
        ProxerConnection.cancel(ProxerTag.CONFERENCES)
    }

    override fun notifyLoadFinishedSuccessful(result: ProxerResult<*>) {
        super.notifyLoadFinishedSuccessful(result)

        pollingModule.onSuccessfulRequest()
    }

    override fun notifyLoadFinishedWithError(result: ProxerErrorResult) {
        super.notifyLoadFinishedWithError(result)

        pollingModule.onErrorRequest()
    }
}