package me.proxer.app.chat.prv.conference.info

import android.app.Activity
import android.os.Bundle
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.chat.prv.LocalConference
import org.jetbrains.anko.startActivity

/**
 * @author Ruben Gees
 */
class ConferenceInfoActivity : DrawerActivity() {

    companion object {
        private const val CONFERENCE_EXTRA = "conference"

        fun navigateTo(context: Activity, conference: LocalConference) {
            context.startActivity<ConferenceInfoActivity>(CONFERENCE_EXTRA to conference)
        }
    }

    val conference: LocalConference
        get() = intent.getParcelableExtra(CONFERENCE_EXTRA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ConferenceInfoFragment.newInstance())
                .commitNow()
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = conference.topic
    }
}
