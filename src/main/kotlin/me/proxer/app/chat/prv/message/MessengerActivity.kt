package me.proxer.app.chat.prv.message

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.transaction
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.conference.info.ConferenceInfoActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.extension.intentFor

/**
 * @author Ruben Gees
 */
class MessengerActivity : DrawerActivity() {

    companion object {
        private const val CONFERENCE_EXTRA = "conference"
        private const val INITIAL_MESSAGE_EXTRA = "initial_message"

        fun navigateTo(context: Activity, conference: LocalConference, initialMessage: String? = null) {
            context.startActivity(
                context.intentFor<MessengerActivity>(
                    CONFERENCE_EXTRA to conference,
                    INITIAL_MESSAGE_EXTRA to initialMessage
                )
            )
        }

        fun getIntent(context: Context, conference: LocalConference, initialMessage: String? = null): Intent {
            return context.intentFor<MessengerActivity>(
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
            supportFragmentManager.transaction(now = true) {
                replace(R.id.container, MessengerFragment.newInstance())
            }
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = conference.topic

        toolbar.clicks()
            .autoDisposable(this.scope())
            .subscribe {
                when (conference.isGroup) {
                    true -> ConferenceInfoActivity.navigateTo(this, conference)
                    false -> ProfileActivity.navigateTo(this, null, conference.topic, conference.image)
                }
            }
    }
}
