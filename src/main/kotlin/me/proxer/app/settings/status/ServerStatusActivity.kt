package me.proxer.app.settings.status

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commitNow
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.util.extension.startActivity

/**
 * @author Ruben Gees
 */
class ServerStatusActivity : AppCompatActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<ServerStatusActivity>()
    }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_default)

        setSupportActionBar(toolbar)
        setTitle(R.string.section_server_status)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, ServerStatusFragment.newInstance())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }

        return super.onOptionsItemSelected(item)
    }
}
