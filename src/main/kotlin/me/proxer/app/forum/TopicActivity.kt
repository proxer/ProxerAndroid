package me.proxer.app.forum

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.subscribeAndLogErrors
import org.jetbrains.anko.startActivity

/**
 * @author Ruben Gees
 */
class TopicActivity : DrawerActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val TOPIC_EXTRA = "topic"

        fun navigateTo(
            context: Activity,
            id: String,
            topic: String? = null
        ) = context.startActivity<TopicActivity>(ID_EXTRA to id, TOPIC_EXTRA to topic)
    }

    val id: String
        get() = when {
            intent.action == Intent.ACTION_VIEW -> intent.data.pathSegments.getOrElse(2, { "-1" })
            else -> intent.getStringExtra(ID_EXTRA)
        }

    var topic: String?
        get() = intent.getStringExtra(TOPIC_EXTRA)
        set(value) {
            intent.putExtra(TOPIC_EXTRA, value)

            title = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, TopicFragment.newInstance())
                    .commitNow()
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = topic

        toolbar.clicks()
                .autoDispose(this)
                .subscribeAndLogErrors {
                    topic?.also { topic ->
                        multilineSnackbar(root, topic)
                    }
                }
    }
}
