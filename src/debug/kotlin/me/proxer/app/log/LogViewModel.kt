package me.proxer.app.log

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Environment
import com.hadisatrio.libs.android.viewmodelprovider.GeneratedProvider
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.R
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.subscribeAndLogErrors
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
@GeneratedProvider
class LogViewModel : ViewModel() {

    val data = MutableLiveData<List<LogMessage>>()
    val saveSuccess = ResettingMutableLiveData<Unit>()
    val saveError = ResettingMutableLiveData<Throwable>()

    private var dataDisposable: Disposable? = null
    private var saveDisposable: Disposable? = null

    init {
        dataDisposable = Observable.interval(1, TimeUnit.SECONDS)
                .startWith(0)
                .map {
                    val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "--format=time", "*:I"))

                    process.inputStream.use { it.reader().readLines() }
                            .filter { it.substringAfter(": ").isNotBlank() }
                            .mapIndexed { index, rawLog -> convertToLogMessage(index, rawLog) }
                            .asReversed()
                            .also { process.waitFor() }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data.value = it }
    }

    override fun onCleared() {
        dataDisposable?.dispose()
        saveDisposable?.dispose()

        dataDisposable = null
        saveDisposable = null

        super.onCleared()
    }

    fun save() {
        saveDisposable?.dispose()
        saveDisposable = Completable
                .fromAction {
                    data.value?.let {
                        val environmentDir = Environment.getExternalStorageDirectory()
                        val dir = File(environmentDir, globalContext.getString(R.string.app_name)).apply { mkdirs() }
                        val file = File(dir, "${Date().time}.log").apply { createNewFile() }

                        file.writeText(it.asReversed().joinToString("\n"))
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    saveSuccess.value = null
                    saveError.value = null
                }
                .subscribeAndLogErrors({
                    saveSuccess.value = Unit
                }, {
                    saveError.value = it
                })
    }

    private fun convertToLogMessage(index: Int, rawLog: String): LogMessage {
        val date = try {
            val rawDate = rawLog.split(" ").let { if (it.size < 2) "" else "${it[0]} ${it[1]}" }

            LocalDateTime.now()
                    .withMonth(rawDate.substringBefore("-").toInt())
                    .withDayOfMonth(rawDate.substringAfter("-").substringBefore(" ").toInt())
                    .withHour(rawDate.substringAfter(" ").substringBefore(":").toInt())
                    .withMinute(rawDate.substringAfter(":").substringBefore(":").toInt())
                    .withSecond(rawDate.substringAfterLast(":").substringBefore(".").toInt())
                    .withNano(rawDate.substringAfter(".").toInt() * 1000)
        } catch (error: NumberFormatException) {
            LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)
        }

        return LogMessage(index.toLong(), rawLog.substringAfter(": ").trimEnd(), date)
    }
}
