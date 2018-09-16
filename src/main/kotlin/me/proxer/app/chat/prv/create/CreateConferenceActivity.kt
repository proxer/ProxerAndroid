package me.proxer.app.chat.prv.create

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.transaction
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.chat.prv.Participant
import me.proxer.app.util.extension.intentFor

/**
 * @author Ruben Gees
 */
class CreateConferenceActivity : DrawerActivity() {

    companion object {
        private const val IS_GROUP_EXTRA = "is_group"
        private const val INITIAL_PARTICIPANT_EXTRA = "initial_participant"

        fun navigateTo(context: Activity, isGroup: Boolean = false, initialParticipant: Participant? = null) {
            context.startActivity(
                context.intentFor<CreateConferenceActivity>(
                    IS_GROUP_EXTRA to isGroup,
                    INITIAL_PARTICIPANT_EXTRA to initialParticipant
                )
            )
        }

        fun getIntent(context: Activity, isGroup: Boolean = false, initialParticipant: Participant? = null): Intent {
            return context.intentFor<CreateConferenceActivity>(
                IS_GROUP_EXTRA to isGroup,
                INITIAL_PARTICIPANT_EXTRA to initialParticipant
            )
        }
    }

    val isGroup: Boolean
        get() = intent.getBooleanExtra(IS_GROUP_EXTRA, false)

    val initialParticipant: Participant?
        get() = intent.getParcelableExtra(INITIAL_PARTICIPANT_EXTRA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager.transaction(now = true) {
                replace(R.id.container, CreateConferenceFragment.newInstance())
            }
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = when (isGroup) {
            true -> getString(R.string.action_create_group)
            false -> getString(R.string.action_create_chat)
        }
    }
}
