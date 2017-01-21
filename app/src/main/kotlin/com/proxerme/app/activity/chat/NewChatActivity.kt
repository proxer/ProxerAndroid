package com.proxerme.app.activity.chat

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.proxerme.app.R
import com.proxerme.app.activity.MainActivity
import com.proxerme.app.entitiy.Participant
import com.proxerme.app.fragment.chat.NewChatFragment
import com.proxerme.app.util.bindView
import org.jetbrains.anko.intentFor

class NewChatActivity : MainActivity() {

    companion object {
        private const val EXTRA_PARTICIPANT = "extra_participant"
        private const val EXTRA_IS_GROUP = "extra_is_group"

        fun navigateTo(context: Activity, initialParticipant: Participant? = null,
                       isGroup: Boolean = false) {
            context.startActivity(context.intentFor<NewChatActivity>(
                    EXTRA_PARTICIPANT to initialParticipant,
                    EXTRA_IS_GROUP to isGroup
            ))
        }
    }

    val initialParticipant: Participant?
        get() = intent.getParcelableExtra(EXTRA_PARTICIPANT)
    val isGroup: Boolean
        get() = intent.getBooleanExtra(EXTRA_IS_GROUP, false)

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = when (isGroup) {
            true -> getString(R.string.action_new_group)
            false -> getString(R.string.action_new_chat)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.container,
                    NewChatFragment.newInstance()).commitNow()
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
