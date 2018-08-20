package me.proxer.app.ui.crash

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import kotterknife.bindView
import linkClicks
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.chat.prv.Participant
import me.proxer.app.chat.prv.create.CreateConferenceActivity
import me.proxer.app.util.extension.linkify

/**
 * @author Ruben Gees
 */
class CrashActivity : BaseActivity() {

    private companion object {
        private const val DEVELOPER_PROXER_NAME = "RubyGee"
    }

    private val config: CaocConfig
        get() = try {
            CustomActivityOnCrash.getConfigFromIntent(intent)
        } catch (ignored: Exception) { // Workaround for a bug in Caoc.
            CaocConfig()
        } ?: CaocConfig()

    private val errorDetails: String
        get() {
            val androidVersion = "Android version: ${Build.VERSION.RELEASE}\n"

            return androidVersion + CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, intent)
        }

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val text: TextView by bindView(R.id.text)
    private val report: Button by bindView(R.id.report)
    private val restart: Button by bindView(R.id.restart)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_crash)
        setSupportActionBar(toolbar)
        title = getString(R.string.section_crash)

        report.clicks()
            .autoDisposable(this.scope())
            .subscribe { CrashDialog.show(this, errorDetails) }

        restart.clicks()
            .autoDisposable(this.scope())
            .subscribe { CustomActivityOnCrash.restartApplication(this, config) }

        text.linkClicks()
            .autoDisposable(this.scope())
            .subscribe {
                CustomActivityOnCrash.restartApplicationWithIntent(
                    this,
                    CreateConferenceActivity.getIntent(this, false, Participant(DEVELOPER_PROXER_NAME)), config
                )
            }

        text.text = getString(R.string.activity_crash_text).linkify(web = false)
    }
}
