package me.proxer.app.chat.prv.conference.info

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.toIconicsSizeDp
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import linkClicks
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.chat.prv.conference.info.ConferenceParticipantAdapter.ViewHolder
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.linkify
import me.proxer.app.util.extension.logErrors
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.app.util.extension.toIconicsColorAttr
import me.proxer.library.entity.messenger.ConferenceParticipant
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl

/**
 * @author Ruben Gees
 */
class ConferenceParticipantAdapter : BaseAdapter<ConferenceParticipant, ViewHolder>() {

    var leaderId: String? = null
    var glide: GlideRequests? = null
    val participantClickSubject: PublishSubject<Pair<ImageView, ConferenceParticipant>> = PublishSubject.create()
    val statusLinkClickSubject: PublishSubject<HttpUrl> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_conference_participant, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val image: ImageView by bindView(R.id.image)
        internal val username: TextView by bindView(R.id.username)
        internal val status: TextView by bindView(R.id.status)

        fun bind(item: ConferenceParticipant) {
            itemView.clicks()
                .mapAdapterPosition({ adapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(participantClickSubject)

            status.linkClicks()
                .map { Utils.parseAndFixUrl(it).toOptional() }
                .filterSome()
                .autoDisposable(this)
                .subscribe(statusLinkClickSubject)

            ViewCompat.setTransitionName(image, "conference_participant_${item.id}")

            username.text = item.username

            if (item.id == leaderId) {
                username.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    IconicsDrawable(username.context)
                        .icon(CommunityMaterial.Icon2.cmd_star)
                        .size(32.toIconicsSizeDp())
                        .padding(8.toIconicsSizeDp())
                        .color(R.attr.colorSecondary.toIconicsColorAttr(image.context)),
                    null
                )
            } else {
                username.setCompoundDrawables(null, null, null, null)
            }

            if (item.status.isBlank()) {
                status.isGone = true
            } else {
                status.isVisible = true
                status.text = item.status.linkify(mentions = false)
            }

            if (item.image.isBlank()) {
                image.setIconicsImage(CommunityMaterial.Icon.cmd_account, 96, 16, R.attr.colorSecondary)
            } else {
                glide?.load(ProxerUrls.userImage(item.image).toString())
                    ?.transition(DrawableTransitionOptions.withCrossFade())
                    ?.circleCrop()
                    ?.logErrors()
                    ?.into(image)
            }
        }
    }
}
