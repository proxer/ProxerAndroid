package me.proxer.app.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.chat.conference.info.ConferenceInfoActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.extension.autoDispose
import org.jetbrains.anko.intentFor

/**
 * @author Ruben Gees
 */
class ChatActivity : DrawerActivity() {

    companion object {
        private const val CONFERENCE_EXTRA = "conference"
        private const val INITIAL_MESSAGE_EXTRA = "initial_message"

        fun navigateTo(context: Activity, conference: LocalConference, initialMessage: String? = null) {
            context.startActivity(context.intentFor<ChatActivity>(
                CONFERENCE_EXTRA to conference,
                INITIAL_MESSAGE_EXTRA to initialMessage
            ))
        }

        fun getIntent(context: Context, conference: LocalConference, initialMessage: String? = null): Intent {
            return context.intentFor<ChatActivity>(
                CONFERENCE_EXTRA to conference,
                INITIAL_MESSAGE_EXTRA to initialMessage
            )
        }
    }

    var conference: LocalConference
        get() = intent.getParcelableExtra(CONFERENCE_EXTRA)
        set(value) {
            intent.putExtra(CONFERENCE_EXTRA, value)

            title = conference.topic
        }

    val initialMessage: String?
        get() = intent.getStringExtra(INITIAL_MESSAGE_EXTRA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ChatFragment.newInstance())
                .commitNow()
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = conference.topic

        toolbar.clicks()
            .autoDispose(this)
            .subscribe {
                when (conference.isGroup) {
                    true -> ConferenceInfoActivity.navigateTo(this, conference)
                    false -> ProfileActivity.navigateTo(this, null, conference.topic, conference.image)
                }
            }
    }
}
