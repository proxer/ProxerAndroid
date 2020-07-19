package me.proxer.app.comment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commitNow
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseActivity
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.multilineSnackbar
import me.proxer.app.util.extension.toast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Ruben Gees
 */
class EditCommentActivity : BaseActivity() {

    companion object {
        const val COMMENT_EXTRA = "comment"

        private const val ID_ARGUMENT = "id"
        private const val ENTRY_ID_ARGUMENT = "entry_id"
        private const val NAME_ARGUMENT = "name_id"
    }

    val id: String?
        get() = intent.getStringExtra(ID_ARGUMENT)

    val entryId: String?
        get() = intent.getStringExtra(ENTRY_ID_ARGUMENT)

    val name: String?
        get() = intent.getStringExtra(NAME_ARGUMENT)

    private val viewModel by viewModel<EditCommentViewModel> {
        parametersOf(id, entryId)
    }

    private val toolbar: Toolbar by bindView(R.id.toolbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_no_drawer)
        setSupportActionBar(toolbar)
        setupToolbar()

        viewModel.isUpdate.observe(
            this,
            Observer {
                title = getString(
                    when (it) {
                        true -> R.string.action_update_comment
                        false -> R.string.action_create_comment
                    }
                )
            }
        )

        viewModel.publishResult.observe(
            this,
            Observer {
                if (it != null) {
                    viewModel.data.value?.also { comment ->
                        setResult(Activity.RESULT_OK, Intent().putExtra(COMMENT_EXTRA, comment))
                    }

                    toast(R.string.fragment_edit_comment_published)

                    finish()
                }
            }
        )

        viewModel.publishError.observe(
            this,
            Observer {
                it?.let {
                    multilineSnackbar(
                        getString(R.string.error_comment_publish, getString(it.message)),
                        Snackbar.LENGTH_LONG, it.buttonMessage, it.toClickListener(this)
                    )
                }
            }
        )

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, EditCommentFragment.newInstance())
            }
        }
    }

    override fun onDestroy() {
        entryId?.also { safeEntryId ->
            val content = viewModel.data.value?.content ?: ""

            if (viewModel.isUpdate.value == false && content.isNotBlank()) {
                storageHelper.putCommentDraft(safeEntryId, content)

                toast(R.string.fragment_edit_comment_draft_saved)
            } else {
                storageHelper.deleteCommentDraft(safeEntryId)
            }
        }

        super.onDestroy()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.subtitle = name?.trim()
    }

    class Contract : ActivityResultContract<Contract.Input, LocalComment>() {

        override fun createIntent(context: Context, input: Input) = context.intentFor<EditCommentActivity>(
            ID_ARGUMENT to input.id,
            ENTRY_ID_ARGUMENT to input.entryId,
            NAME_ARGUMENT to input.name
        )

        override fun parseResult(resultCode: Int, intent: Intent?): LocalComment? {
            if (resultCode != Activity.RESULT_OK) {
                return null
            }

            return intent?.getParcelableExtra(COMMENT_EXTRA)
        }

        data class Input(val id: String? = null, val entryId: String? = null, val name: String? = null)
    }
}
