package me.proxer.app.chat.prv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.fragment.app.commitNow
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.chat.prv.conference.ConferenceFragment
import me.proxer.app.chat.prv.message.MessengerFragment
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.startActivity
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * @author Ruben Gees
 */
class PrvMessengerActivity : DrawerActivity() {

    companion object {
        private const val CONFERENCE_EXTRA = "conference"

        fun navigateTo(context: Activity, conference: LocalConference, initialMessage: String? = null) {
            context.startActivity<PrvMessengerActivity>(
                CONFERENCE_EXTRA to conference,
                Intent.EXTRA_TEXT to initialMessage
            )
        }

        fun getIntent(context: Context, conference: LocalConference, initialMessage: String? = null): Intent {
            return context.intentFor<PrvMessengerActivity>(
                CONFERENCE_EXTRA to conference,
                Intent.EXTRA_TEXT to initialMessage
            )
        }

        fun getIntent(context: Context, conferenceId: String, initialMessage: String? = null): Intent {
            return context.intentFor<PrvMessengerActivity>(
                ShortcutManagerCompat.EXTRA_SHORTCUT_ID to conferenceId,
                Intent.EXTRA_TEXT to initialMessage
            )
        }
    }

    private val messengerDao by inject<MessengerDao>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (supportFragmentManager.fragments.isEmpty()) {
            val conference = intent.getParcelableExtra<LocalConference>(CONFERENCE_EXTRA)
            val initialMessage = intent.getStringExtra(Intent.EXTRA_TEXT)

            if (conference != null) {
                supportFragmentManager.commitNow {
                    replace(R.id.container, MessengerFragment.newInstance(conference, initialMessage))
                }
            } else {
                val conferenceId = intent.getStringExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID)?.toLongOrNull()

                if (conferenceId != null) {
                    title = getString(R.string.fragment_chat_loading_message)

                    messengerDao.getConferenceMaybe(conferenceId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .autoDisposable(this.scope())
                        .subscribe(
                            Consumer {
                                if (it != null) {
                                    supportFragmentManager.commitNow {
                                        replace(R.id.container, MessengerFragment.newInstance(it, initialMessage))
                                    }
                                } else {
                                    error("No conference found for id $conferenceId")
                                }
                            },
                            Consumer {
                                Timber.e(it)

                                finish()
                            })
                } else {
                    title = getString(R.string.activity_prv_messenger_send_to)

                    supportFragmentManager.commitNow {
                        replace(R.id.container, ConferenceFragment.newInstance(initialMessage))
                    }
                }
            }
        }
    }
}
