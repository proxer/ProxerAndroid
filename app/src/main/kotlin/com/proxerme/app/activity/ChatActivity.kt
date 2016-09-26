package com.proxerme.app.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.fragment.chat.ChatFragment
import org.jetbrains.anko.intentFor

class ChatActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CONFERENCE = "extra_conference"

        fun navigateTo(context: Activity, conference: LocalConference) {
            context.startActivity(context.intentFor<ChatActivity>(EXTRA_CONFERENCE to conference))
        }

        fun getIntent(context: Context, conference: LocalConference): Intent {
            return context.intentFor<ChatActivity>(EXTRA_CONFERENCE to conference)
        }
    }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = intent.getParcelableExtra<LocalConference>(EXTRA_CONFERENCE).topic

        toolbar.setOnClickListener {
            val conference = intent.getParcelableExtra<LocalConference>(EXTRA_CONFERENCE)

            if (conference.isGroup) {
                ConferenceInfoActivity.navigateTo(this, conference)
            } else {
                UserActivity.navigateTo(this, null, conference.topic, conference.imageId)
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.container,
                    ChatFragment.newInstance(intent.getParcelableExtra(EXTRA_CONFERENCE)))
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
