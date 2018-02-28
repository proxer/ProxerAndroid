package me.proxer.app.manga

import android.app.Dialog
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.unsafeLazy

/**
 * @author Ruben Gees
 */
class MangaCleanDialog : BaseDialog() {

    companion object {
        fun show(activity: AppCompatActivity) {
            MangaCleanDialog().show(activity.supportFragmentManager, "clean_manga_dialog")
        }
    }

    private val viewModel by unsafeLazy { MangaCleanViewModelProvider.get(this) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog.Builder(requireContext())
            .content(R.string.dialog_clean_manga_content)
            .positiveText(R.string.dialog_clean_manga_positive)
            .negativeText(R.string.cancel)
            .onPositive { _, _ -> viewModel.clean() }
            .build()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.data.observe(this, Observer {
            it?.let { dismiss() }
        })
    }
}
