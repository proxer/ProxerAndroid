package com.proxerme.app.activity.chat

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.proxerme.app.R
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.fragment.chat.ConferenceInfoFragment
import com.proxerme.app.util.bindView
import org.jetbrains.anko.startActivity

class ConferenceInfoActivity : MainActivity() {

    companion object {
        private const val EXTRA_CONFERENCE = "extra_conference"

        fun navigateTo(context: Activity, conference: LocalConference) {
            context.startActivity<ConferenceInfoActivity>(EXTRA_CONFERENCE to conference)
        }
    }

    val conference: LocalConference
        get() = intent.getParcelableExtra(EXTRA_CONFERENCE)

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = conference.topic

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.container,
                    ConferenceInfoFragment.newInstance(conference.id)).commitNow()
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
