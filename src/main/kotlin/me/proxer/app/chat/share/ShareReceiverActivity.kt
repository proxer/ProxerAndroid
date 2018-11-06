package me.proxer.app.chat.share

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commitNow
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.message.MessengerActivity
import me.proxer.app.util.extension.unsafeLazy

/**
 * @author Ruben Gees
 */
class ShareReceiverActivity : BaseActivity() {

    val text: String by unsafeLazy {
        intent.getStringExtra(Intent.EXTRA_TEXT)
    }

    val conference: LocalConference? by unsafeLazy {
        intent.getBundleExtra(ConferenceChooserTargetService.ARGUMENT_CONFERENCE_WRAPPER)
            ?.getParcelable<LocalConference>(ConferenceChooserTargetService.ARGUMENT_CONFERENCE)
    }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val safeConference = conference

            if (safeConference != null) {
                MessengerActivity.navigateTo(this, safeConference, text)

                finish()
            } else {
                setContentView(R.layout.activity_default)
                setSupportActionBar(toolbar)

                supportActionBar?.setDisplayHomeAsUpEnabled(true)

                if (savedInstanceState == null) {
                    supportFragmentManager.commitNow {
                        replace(R.id.container, ShareReceiverFragment.newInstance())
                    }
                }
            }
        } else {
            finish()
        }
    }
}
