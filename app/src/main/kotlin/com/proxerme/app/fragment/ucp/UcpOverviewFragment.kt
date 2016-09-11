package com.proxerme.app.fragment.ucp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.bindView
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.TouchableMovementMethod
import com.proxerme.app.R
import com.proxerme.app.fragment.framework.EasyLoadingFragment
import com.proxerme.app.manager.SectionManager.Section
import com.proxerme.app.manager.UserManager
import com.proxerme.app.module.LoginModule
import com.proxerme.app.util.Utils
import com.proxerme.library.connection.ucp.request.ListsumRequest
import com.proxerme.library.info.ProxerUrlHolder
import org.jetbrains.anko.toast

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class UcpOverviewFragment : EasyLoadingFragment<Int>() {

    companion object {
        private const val STATE_WATCHED_EPISODES = "fragment_ucp_overview_state_watched_episodes"
        private const val FORMAT = "%.1f"

        fun newInstance(): UcpOverviewFragment {
            return UcpOverviewFragment()
        }
    }

    private val loginModule = LoginModule(object : LoginModule.LoginModuleCallback {
        override val activity: AppCompatActivity
            get() = this@UcpOverviewFragment.activity as AppCompatActivity

        override fun showError(message: String, buttonMessage: String?,
                               onButtonClickListener: View.OnClickListener?) {
            this@UcpOverviewFragment.doShowError(message, buttonMessage, onButtonClickListener)
        }

        override fun load(showProgress: Boolean) {
            this@UcpOverviewFragment.load()
        }
    })

    override val section = Section.UCP_OVERVIEW
    override val canLoad: Boolean
        get() = super.canLoad && loginModule.canLoad()

    private var watchedEpisodes: Int? = null

    private val profileLink: TextView by bindView(R.id.profileLink)
    private val username: TextView by bindView(R.id.username)
    private val userId: TextView by bindView(R.id.userId)

    private val episodesRow: TextView by bindView(R.id.episodesRow)
    private val minutesRow: TextView by bindView(R.id.minutesRow)
    private val hoursRow: TextView by bindView(R.id.hoursRow)
    private val daysRow: TextView by bindView(R.id.daysRow)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            if (savedInstanceState.containsKey(STATE_WATCHED_EPISODES)) {
                watchedEpisodes = it.getInt(STATE_WATCHED_EPISODES)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        loginModule.onStart()
    }

    override fun onResume() {
        super.onResume()

        loginModule.onResume()
    }

    override fun onStop() {
        loginModule.onStop()

        super.onStop()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_ucp_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileLink.movementMethod = TouchableMovementMethod.getInstance()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        watchedEpisodes?.let {
            outState.putInt(STATE_WATCHED_EPISODES, it)
        }
    }

    override fun save(result: Int) {
        watchedEpisodes = result
    }

    override fun show() {
        if (watchedEpisodes != null && UserManager.user != null) {
            profileLink.text = Utils.buildClickableText(context,
                    ProxerUrlHolder.getUserUrl(UserManager.user!!.id, null).toString(),
                    onWebClickListener = Link.OnClickListener { link ->
                        Utils.viewLink(context, link)
                    },
                    onWebLongClickListener = Link.OnLongClickListener { link ->
                        Utils.setClipboardContent(activity,
                                getString(R.string.fragment_ucp_overview_clip_title), link)

                        context.toast(R.string.clipboard_status)
                    })
            username.text = UserManager.user!!.username
            userId.text = UserManager.user!!.id

            episodesRow.text = watchedEpisodes.toString()

            val minutes = watchedEpisodes!! * 20
            val hours = minutes / 60f
            val days = hours / 24f

            minutesRow.text = minutes.toString()
            hoursRow.text = FORMAT.format(hours)
            daysRow.text = FORMAT.format(days)
        }
    }

    override fun clear() {
        watchedEpisodes = null
    }

    override fun constructLoadingRequest(): LoadingRequest<Int> {
        return LoadingRequest(ListsumRequest())
    }
}