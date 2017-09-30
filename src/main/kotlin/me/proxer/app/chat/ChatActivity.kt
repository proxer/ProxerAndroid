package me.proxer.app.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.AndroidLifecycle
import com.uber.autodispose.kotlin.autoDisposeWith
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.chat.conference.info.ConferenceInfoActivity
import me.proxer.app.profile.ProfileActivity
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.startActivity

class ChatActivity : BaseActivity() {

    companion object {
        private const val CONFERENCE_EXTRA = "conference"

        fun navigateTo(context: Activity, conference: LocalConference) {
            context.startActivity<ChatActivity>(CONFERENCE_EXTRA to conference)
        }

        fun getIntent(context: Context, conference: LocalConference): Intent {
            return context.intentFor<ChatActivity>(CONFERENCE_EXTRA to conference)
        }
    }

    var conference: LocalConference
        get() = intent.getParcelableExtra(CONFERENCE_EXTRA)
        set(value) {
            intent.putExtra(CONFERENCE_EXTRA, value)

            title = conference.topic
        }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, ChatFragment.newInstance())
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

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = conference.topic

        toolbar.clicks()
                .autoDisposeWith(AndroidLifecycle.from(this))
                .subscribe {
                    when (conference.isGroup) {
                        true -> ConferenceInfoActivity.navigateTo(this, conference)
                        false -> ProfileActivity.navigateTo(this, null, conference.topic, conference.image)
                    }
                }
    }
}
