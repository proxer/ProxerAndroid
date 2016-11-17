package com.proxerme.app.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import butterknife.bindView
import com.proxerme.app.R
import org.joda.time.DateTime

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class MediaControlView(context: Context?, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private companion object {
        private const val DATE_PATTERN = "dd.MM.yyyy"
    }

    var onUploaderClickListener: (() -> Unit)? = null
    var onTranslatorGroupClickListener: (() -> Unit)? = null
    var onSwitchClickListener: ((episode: Int) -> Unit)? = null
    var onReminderClickListener: ((episode: Int) -> Unit)? = null

    var textResolver: TextResourceResolver?
        set(value) {
            if (value != null) {
                previous.text = value.previous()
                next.text = value.next()
                reminderThis.text = value.reminderThis()
                reminderNext.text = value.reminderNext()
            }
        }
        get() = null

    private val uploaderRow: ViewGroup by bindView(R.id.uploaderRow)
    private val translatorRow: ViewGroup by bindView(R.id.translatorRow)
    private val dateRow: ViewGroup by bindView(R.id.dateRow)

    private val uploaderText: TextView by bindView(R.id.uploader)
    private val translatorGroup: TextView by bindView(R.id.translatorGroup)
    private val dateText: TextView by bindView(R.id.date)

    private val previous: Button by bindView(R.id.previous)
    private val next: Button by bindView(R.id.next)
    private val reminderThis: Button by bindView(R.id.reminderThis)
    private val reminderNext: Button by bindView(R.id.reminderNext)

    init {
        LayoutInflater.from(context).inflate(R.layout.view_media_control, this, true)

        uploaderText.setOnClickListener { onUploaderClickListener?.invoke() }
        translatorGroup.setOnClickListener { onTranslatorGroupClickListener?.invoke() }
    }

    fun setUploader(uploader: String?) {
        if (uploader == null) {
            uploaderRow.visibility = View.GONE
        } else {
            uploaderRow.visibility = View.VISIBLE
            uploaderText.text = uploader
        }
    }

    fun setTranslatorGroup(group: String?) {
        if (group == null) {
            translatorRow.visibility = View.GONE
        } else {
            translatorRow.visibility = View.VISIBLE
            translatorGroup.text = group
        }
    }

    fun setDate(date: DateTime?) {
        if (date == null) {
            dateRow.visibility = View.GONE
        } else {
            dateRow.visibility = View.VISIBLE
            dateText.text = date.toString(DATE_PATTERN)
        }
    }

    fun setEpisodeInfo(totalEpisodes: Int, currentEpisode: Int) {
        if (currentEpisode <= 1) {
            previous.visibility = View.GONE
        } else {
            previous.visibility = View.VISIBLE
            previous.setOnClickListener {
                onSwitchClickListener?.invoke(currentEpisode - 1)
            }
        }

        if (currentEpisode >= totalEpisodes) {
            next.visibility = View.GONE
            reminderNext.visibility = View.GONE
        } else {
            next.visibility = View.VISIBLE
            next.setOnClickListener {
                onSwitchClickListener?.invoke(currentEpisode + 1)
            }

            reminderNext.visibility = View.VISIBLE
            reminderNext.setOnClickListener {
                onReminderClickListener?.invoke(currentEpisode + 1)
            }
        }

        reminderThis.setOnClickListener {
            onReminderClickListener?.invoke(currentEpisode)
        }
    }

    interface TextResourceResolver {
        fun next(): String
        fun previous(): String
        fun reminderThis(): String
        fun reminderNext(): String
    }
}