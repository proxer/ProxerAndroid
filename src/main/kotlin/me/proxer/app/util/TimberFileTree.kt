package me.proxer.app.util

import android.content.Context
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.io.File

class TimberFileTree(private val context: Context) : Timber.Tree() {

    private companion object {
        private val FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private const val LOGS_DIRECTORY = "logs"
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Completable
            .fromAction {
                val logDir = File(context.getExternalFilesDir(null), LOGS_DIRECTORY).also { it.mkdirs() }
                val logFile = File(logDir, "${LocalDate.now()}.log").also { it.createNewFile() }
                val currentTime = LocalDateTime.now().format(FORMAT)

                val maybeTag = if (tag != null) "$tag: " else ""
                val maybeNewline = if (message.endsWith("\n")) "" else "\n"

                logFile.appendText("$currentTime  $maybeTag$message$maybeNewline")
            }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }
}
