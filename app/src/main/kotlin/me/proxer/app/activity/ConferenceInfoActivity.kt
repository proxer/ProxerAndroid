package me.proxer.app.activity

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import me.proxer.app.R
import me.proxer.app.activity.base.MainActivity
import me.proxer.app.entity.chat.LocalConference
import me.proxer.app.fragment.chat.ConferenceInfoFragment
import me.proxer.app.util.extension.bindView
import org.jetbrains.anko.startActivity

class ConferenceInfoActivity : MainActivity() {

    companion object {
        private const val CONFERENCE_EXTRA = "conference"

        fun navigateTo(context: Activity, conference: LocalConference) {
            context.startActivity<ConferenceInfoActivity>(CONFERENCE_EXTRA to conference)
        }
    }

    val conference: LocalConference
        get() = intent.getParcelableExtra(CONFERENCE_EXTRA)

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = conference.topic

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .setAllowOptimization(true)
                    .replace(R.id.container, ConferenceInfoFragment.newInstance())
                    .commitNow()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
