package me.proxer.app.forum

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ShareCompat
import android.view.Menu
import android.view.MenuItem
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.kotlin.autoDisposable
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.subscribeAndLogErrors
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.startActivity

/**
 * @author Ruben Gees
 */
class TopicActivity : DrawerActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val CATEGORY_ID_EXTRA = "category_id"
        private const val TOPIC_EXTRA = "topic"

        fun navigateTo(context: Activity, id: String, categoryId: String, topic: String? = null) {
            context.startActivity<TopicActivity>(
                ID_EXTRA to id,
                CATEGORY_ID_EXTRA to categoryId,
                TOPIC_EXTRA to topic
            )
        }

        fun getIntent(context: Context, id: String, categoryId: String, topic: String? = null): Intent {
            return context.intentFor<TopicActivity>(
                ID_EXTRA to id,
                CATEGORY_ID_EXTRA to categoryId,
                TOPIC_EXTRA to topic
            )
        }
    }

    val id: String
        get() = when {
            intent.action == Intent.ACTION_VIEW -> intent.data?.pathSegments?.getOrElse(2) { "-1" } ?: "-1"
            else -> intent.getStringExtra(ID_EXTRA)
        }

    val categoryId: String
        get() = when {
            intent.action == Intent.ACTION_VIEW -> intent.data?.pathSegments?.getOrElse(1) { "-1" } ?: "-1"
            else -> intent.getStringExtra(CATEGORY_ID_EXTRA)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_share, menu, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> topic?.let {
                val url = when {
                    intent.action == Intent.ACTION_VIEW -> intent.dataString
                    else -> "https://proxer.me/forum/$categoryId/$id"
                }

                ShareCompat.IntentBuilder
                    .from(this)
                    .setText(getString(R.string.share_topic, it, url))
                    .setType("text/plain")
                    .setChooserTitle(getString(R.string.share_title))
                    .startChooser()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = topic

        toolbar.clicks()
            .autoDisposable(this.scope())
            .subscribeAndLogErrors {
                topic?.also { topic ->
                    multilineSnackbar(root, topic)
                }
            }
    }
}
