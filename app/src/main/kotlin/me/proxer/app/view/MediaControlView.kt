package me.proxer.app.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import me.proxer.app.R
import me.proxer.app.util.extension.bindView
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * @author Ruben Gees
 */
class MediaControlView(context: Context?, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }

    var callback: MediaControlViewCallback? = null
    var textResolver: TextResourceResolver? = null
        set(value) {
            field = value

            if (value != null) {
                previous.text = value.previous()
                next.text = value.next()
                bookmarkThis.text = value.bookmarkThis()
                bookmarkNext.text = value.bookmarkNext()
            }
        }
        get() = field

    private val uploaderRow: ViewGroup by bindView(R.id.uploaderRow)
    private val translatorRow: ViewGroup by bindView(R.id.translatorRow)
    private val dateRow: ViewGroup by bindView(R.id.dateRow)

    private val uploaderText: TextView by bindView(R.id.uploader)
    private val translatorGroupText: TextView by bindView(R.id.translatorGroup)
    private val dateText: TextView by bindView(R.id.date)

    private val previous: Button by bindView(R.id.previous)
    private val next: Button by bindView(R.id.next)
    private val bookmarkThis: Button by bindView(R.id.bookmarkThis)
    private val bookmarkNext: Button by bindView(R.id.bookmarkNext)

    init {
        LayoutInflater.from(context).inflate(R.layout.view_media_control, this, true)

        setUploader(null)
        setDateTime(null)
        setTranslatorGroup(null)
    }

    fun setUploader(uploader: Uploader?) {
        if (uploader == null) {
            uploaderRow.visibility = View.GONE

            uploaderText.setOnClickListener(null)
        } else {
            uploaderRow.visibility = View.VISIBLE
            uploaderText.text = uploader.name

            uploaderText.setOnClickListener {
                callback?.onUploaderClick(uploader)
            }
        }
    }

    fun setTranslatorGroup(group: SimpleTranslatorGroup?) {
        if (group == null) {
            translatorRow.visibility = View.GONE

            translatorGroupText.setOnClickListener(null)
        } else {
            translatorRow.visibility = View.VISIBLE
            translatorGroupText.text = group.name

            translatorGroupText.setOnClickListener {
                callback?.onTranslatorGroupClick(group)
            }
        }
    }

    fun setDateTime(dateTime: LocalDateTime?) {
        if (dateTime == null) {
            dateRow.visibility = View.GONE
        } else {
            dateRow.visibility = View.VISIBLE
            dateText.text = DATE_TIME_FORMATTER.format(dateTime)
        }
    }

    fun setEpisodeInfo(episodeAmount: Int, currentEpisode: Int) {
        if (currentEpisode <= 1) {
            previous.visibility = View.GONE
        } else {
            previous.visibility = View.VISIBLE

            previous.setOnClickListener {
                callback?.onSwitchEpisodeClick(currentEpisode - 1)
            }
        }

        if (currentEpisode >= episodeAmount) {
            next.visibility = View.GONE
        } else {
            next.visibility = View.VISIBLE

            next.setOnClickListener {
                callback?.onSwitchEpisodeClick(currentEpisode + 1)
            }
        }

        bookmarkNext.text = when {
            currentEpisode < episodeAmount -> textResolver?.bookmarkNext()
            else -> bookmarkNext.context.getString(R.string.view_media_control_finish)
        }

        bookmarkNext.setOnClickListener {
            if (currentEpisode < episodeAmount) {
                callback?.onSetBookmarkClick(currentEpisode + 1)
            } else {
                callback?.onFinishClick(currentEpisode)
            }
        }

        bookmarkThis.setOnClickListener {
            callback?.onSetBookmarkClick(currentEpisode)
        }
    }

    class Uploader(val id: String, val name: String)
    class SimpleTranslatorGroup(val id: String, val name: String)

    interface TextResourceResolver {
        fun next(): String
        fun previous(): String
        fun bookmarkThis(): String
        fun bookmarkNext(): String
    }

    interface MediaControlViewCallback {
        fun onUploaderClick(uploader: Uploader) {}
        fun onTranslatorGroupClick(group: SimpleTranslatorGroup) {}
        fun onSwitchEpisodeClick(newEpisode: Int) {}
        fun onSetBookmarkClick(episode: Int) {}
        fun onFinishClick(episode: Int) {}
    }
}
