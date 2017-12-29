package me.proxer.app.forum

import android.app.Activity
import android.os.Bundle
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
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
                topic: String?
        ) = context.startActivity<TopicActivity>(ID_EXTRA to id, TOPIC_EXTRA to topic)
    }

    val id: String
        get() = intent.getStringExtra(ID_EXTRA)

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
    }
}
