package me.proxer.app.media.discussion

import android.graphics.Typeface
import android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.media.discussion.DiscussionAdapter.ViewHolder
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.library.entity.info.ForumDiscussion

/**
 * @author Ruben Gees
 */
class DiscussionAdapter : BaseAdapter<ForumDiscussion, ViewHolder>() {

    val clickSubject: PublishSubject<ForumDiscussion> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_discussion, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val subject: TextView by bindView(R.id.subject)
        internal val metaInfo: TextView by bindView(R.id.metaInfo)

        fun bind(item: ForumDiscussion) {
            itemView.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            val metaInfoText = metaInfo.context.getString(R.string.fragment_discussion_meta_info,
                item.firstPostUsername, item.category)

            subject.text = item.subject
            metaInfo.text = SpannableString(metaInfoText).apply {
                val usernameSpanStart = indexOf(item.firstPostUsername)
                val usernameSpanEnd = usernameSpanStart + item.firstPostUsername.length
                val categorySpanStart = indexOf(item.category)
                val categorySpanEnd = categorySpanStart + item.category.length

                this.setSpan(StyleSpan(Typeface.BOLD), usernameSpanStart, usernameSpanEnd, SPAN_INCLUSIVE_EXCLUSIVE)
                this.setSpan(StyleSpan(Typeface.BOLD), categorySpanStart, categorySpanEnd, SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
