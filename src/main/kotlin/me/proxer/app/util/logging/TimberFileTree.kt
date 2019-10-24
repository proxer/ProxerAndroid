package me.proxer.app.util.logging

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.Executors

/**
 * @author Ruben Gees
 */
class TimberFileTree(context: Context) : Timber.Tree() {

    private companion object {
        private const val LOGS_DIRECTORY = "logs"
        private const val ROTATION_THRESHOLD = 7L

        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val executor = Executors.newSingleThreadExecutor()
    }

    private val resolvedLogsDirectory = File(context.getExternalFilesDir(null), LOGS_DIRECTORY)
        .also { it.mkdirs() }

    override fun isLoggable(tag: String?, priority: Int) = priority >= Log.INFO

    @SuppressLint("CheckResult", "LogNotTimber")
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        Completable
            .fromAction { internalLog(tag, message) }
            .subscribeOn(Schedulers.from(executor))
            .subscribe({}, {
                Log.e(TimberFileTree::class.java.name, "Failure while logging to file", it)
            })
    }

    @SuppressLint("LogNotTimber")
    private fun internalLog(tag: String?, message: String) {
        val currentLogFiles = resolvedLogsDirectory.listFiles() ?: emptyArray()
        val currentDateTime = LocalDateTime.now()
        val rotationThresholdDate = currentDateTime.toLocalDate().minusDays(ROTATION_THRESHOLD)

        for (currentLogFile in currentLogFiles) {
            val fileDate = try {
                LocalDate.parse(currentLogFile.nameWithoutExtension)
            } catch (error: DateTimeParseException) {
                Log.e(TimberFileTree::class.java.name, "Invalid log file $currentLogFile found, deleting", error)

                null
            }

            if (fileDate == null || fileDate.isBefore(rotationThresholdDate)) {
                currentLogFile.deleteRecursively()
            }
        }

        val logFile = File(resolvedLogsDirectory, "${LocalDate.now()}.log").also { it.createNewFile() }

        val currentDateTimeText = currentDateTime.format(dateTimeFormatter)
        val maybeTag = if (tag != null) "$tag: " else ""

        logFile.appendText("$currentDateTimeText  $maybeTag${message.trim()}\n")
    }
}
