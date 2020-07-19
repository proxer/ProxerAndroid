package me.proxer.app.forum

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ShareCompat
import androidx.fragment.app.commitNow
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class TopicActivity : DrawerActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val CATEGORY_ID_EXTRA = "category_id"
        private const val TOPIC_EXTRA = "topic"

        private const val TOUZAI_PATH = "/touzai"
        private const val TOUZAI_CATEGORY = "310"

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
        get() = when (intent.hasExtra(ID_EXTRA)) {
            true -> intent.getSafeStringExtra(ID_EXTRA)
            false -> when (intent.data?.path == TOUZAI_PATH) {
                true -> intent.data?.getQueryParameter("id") ?: "-1"
                else -> intent.data?.pathSegments?.getOrNull(2) ?: "-1"
            }
        }

    val categoryId: String
        get() = when (intent.hasExtra(CATEGORY_ID_EXTRA)) {
            true -> intent.getSafeStringExtra(CATEGORY_ID_EXTRA)
            false -> when (intent.data?.path == TOUZAI_PATH) {
                true -> TOUZAI_CATEGORY
                else -> intent.data?.pathSegments?.getOrNull(1) ?: "-1"
            }
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
            supportFragmentManager.commitNow {
                replace(R.id.container, TopicFragment.newInstance())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        IconicsMenuInflaterUtil.inflate(menuInflater, this, R.menu.activity_share, menu, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share ->
                topic
                    ?.let {
                        val url = when (intent.action) {
                            Intent.ACTION_VIEW -> intent.dataString
                            else -> ProxerUrls.forumWeb(categoryId, id).toString()
                        }

                        it to url
                    }
                    ?.let { (topic, url) ->
                        ShareCompat.IntentBuilder
                            .from(this)
                            .setText(getString(R.string.share_topic, topic, url))
                            .setType("text/plain")
                            .setChooserTitle(getString(R.string.share_title))
                            .startChooser()
                    }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        title = topic

        toolbar.clicks()
            .autoDisposable(this.scope())
            .subscribeAndLogErrors {
                topic?.also { topic ->
                    multilineSnackbar(topic)
                }
            }
    }
}
