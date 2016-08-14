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
import com.proxerme.app.fragment.ChatFragment
import com.proxerme.library.connection.messenger.entity.Conference

class ChatActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CONFERENCE = "extra_conference"

        fun navigateTo(context: Activity, conference: Conference) {
            context.startActivity(Intent(context, ChatActivity::class.java).apply {
                this.putExtra(EXTRA_CONFERENCE, conference)
            })
        }

        fun getIntent(context: Context, conference: Conference): Intent {
            return Intent(context, ChatActivity::class.java).apply {
                this.putExtra(EXTRA_CONFERENCE, conference)
            }
        }
    }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_chat)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = intent.getParcelableExtra<Conference>(EXTRA_CONFERENCE).topic

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.contentContainer,
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
