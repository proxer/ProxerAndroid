package me.proxer.app.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import me.proxer.app.R
import me.proxer.app.application.MainApplication
import me.proxer.app.job.LocalMangaJob
import me.proxer.app.task.manga.MangaRemovalTask
import org.jetbrains.anko.doAsync

/**
 * @author Ruben Gees
 */
class CleanMangaDialog : DialogFragment() {

    companion object {
        fun show(activity: AppCompatActivity) {
            CleanMangaDialog().show(activity.supportFragmentManager, "clean_manga_dialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog.Builder(context)
                .content(R.string.dialog_clean_manga_content)
                .positiveText(R.string.dialog_clean_manga_positive)
                .negativeText(R.string.cancel)
                .onPositive { _, _ ->
                    val filesDir = context.filesDir

                    doAsync {
                        LocalMangaJob.cancelAll()
                        MainApplication.mangaDb.clear()
                        MangaRemovalTask(filesDir).execute(Unit)
                    }
                }
                .build()
    }
}