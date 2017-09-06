package me.proxer.app.chat.new

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.chat.Participant
import org.jetbrains.anko.intentFor

class NewChatActivity : BaseActivity() {

    companion object {
        private const val IS_GROUP_EXTRA = "is_group"
        private const val INITIAL_PARTICIPANT_EXTRA = "initial_participant"

        fun navigateTo(context: Activity, isGroup: Boolean = false, initialParticipant: Participant? = null) {
            context.startActivity(context.intentFor<NewChatActivity>(
                    IS_GROUP_EXTRA to isGroup,
                    INITIAL_PARTICIPANT_EXTRA to initialParticipant
            ))
        }

        fun getIntent(context: Activity, isGroup: Boolean = false, initialParticipant: Participant? = null): Intent {
            return context.intentFor<NewChatActivity>(
                    IS_GROUP_EXTRA to isGroup,
                    INITIAL_PARTICIPANT_EXTRA to initialParticipant
            )
        }
    }

    val isGroup: Boolean
        get() = intent.getBooleanExtra(IS_GROUP_EXTRA, false)

    val initialParticipant: Participant?
        get() = intent.getParcelableExtra(INITIAL_PARTICIPANT_EXTRA)

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)
        setSupportActionBar(toolbar)

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, NewChatFragment.newInstance())
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
        title = when (isGroup) {
            true -> getString(R.string.action_new_group)
            false -> getString(R.string.action_new_chat)
        }
    }
}
