package me.proxer.app.anime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.gojuno.koptional.rxjava2.filterSome
import com.gojuno.koptional.toOptional
import com.jakewharton.rxbinding3.view.clicks
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.paddingDp
import com.mikepenz.iconics.utils.sizeDp
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.anime.resolver.ProxerStreamResolver
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.ui.view.BetterLinkTextView
import me.proxer.app.util.Utils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.colorAttr
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.linkClicks
import me.proxer.app.util.extension.mapBindingAdapterPosition
import me.proxer.app.util.extension.toLocalDateTime
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit

/**
 * @author Ruben Gees
 */
class AnimeAdapter(
    savedInstanceState: Bundle?,
    private val storageHelper: StorageHelper
) : BaseAdapter<AnimeStream, RecyclerView.ViewHolder>() {

    private companion object {
        private const val EXPANDED_ITEM_STATE = "anime_stream_expanded_id"
        private const val STREAM_VIEW_TYPE = 100
        private const val MESSAGE_VIEW_TYPE = 101
    }

    var glide: GlideRequests? = null
    val uploaderClickSubject: PublishSubject<AnimeStream> = PublishSubject.create()
    val translatorGroupClickSubject: PublishSubject<AnimeStream> = PublishSubject.create()
    val playClickSubject: PublishSubject<AnimeStream> = PublishSubject.create()
    val loginClickSubject: PublishSubject<AnimeStream> = PublishSubject.create()
    val linkClickSubject: PublishSubject<HttpUrl> = PublishSubject.create()
    val setAdIntervalClickSubject: PublishSubject<Int> = PublishSubject.create()

    private var expandedItemId: String?

    init {
        expandedItemId = when (savedInstanceState) {
            null -> null
            else -> savedInstanceState.getString(EXPANDED_ITEM_STATE)
        }

        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = data[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        STREAM_VIEW_TYPE -> StreamViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_stream, parent, false)
        )

        MESSAGE_VIEW_TYPE -> MessageViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_stream_message, parent, false)
        )

        else -> error("Unknown viewType: $viewType")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is StreamViewHolder -> holder.bind(data[position])
            is MessageViewHolder -> holder.bind(data[position])
            else -> error("Unknown ViewHolder: ${holder::class.java.name}")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val resolutionResult = data[position].resolutionResult

        return when (resolutionResult is StreamResolutionResult.Message) {
            true -> MESSAGE_VIEW_TYPE
            false -> STREAM_VIEW_TYPE
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is StreamViewHolder) {
            glide?.clear(holder.image)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    override fun saveInstanceState(outState: Bundle) = outState.putString(EXPANDED_ITEM_STATE, expandedItemId)

    override fun swapDataAndNotifyWithDiffing(newData: List<AnimeStream>) {
        if (data.isNotEmpty()) {
            expandedItemId = null
        }

        super.swapDataAndNotifyWithDiffing(newData)
    }

    inner class StreamViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val nameContainer: ViewGroup by bindView(R.id.nameContainer)
        internal val name: TextView by bindView(R.id.name)
        internal val image: ImageView by bindView(R.id.image)

        internal val uploadInfoContainer: ViewGroup by bindView(R.id.uploadInfoContainer)
        internal val uploaderText: TextView by bindView(R.id.uploader)
        internal val translatorGroup: TextView by bindView(R.id.translatorGroup)
        internal val dateText: TextView by bindView(R.id.date)

        internal val adAlert: ViewGroup by bindView(R.id.adAlert)
        internal val dismissAdAlert: Button by bindView(R.id.dismissAdAlert)
        internal val setAdInterval: Button by bindView(R.id.setAdInterval)

        internal val info: TextView by bindView(R.id.info)
        internal val play: Button by bindView(R.id.play)

        fun bind(item: AnimeStream) {
            val isLoginRequired = !item.isPublic && !storageHelper.isLoggedIn

            initListeners(isLoginRequired)

            name.text = item.hosterName

            glide?.defaultLoad(image, ProxerUrls.hosterImage(item.image))

            if (expandedItemId == item.id) {
                uploadInfoContainer.isVisible = true
            } else {
                uploadInfoContainer.isVisible = false

                return
            }

            uploaderText.text = item.uploaderName
            translatorGroup.text = item.translatorGroupName
                ?: translatorGroup.context.getString(R.string.fragment_anime_empty_subgroup)

            dateText.text = item.date.toLocalDateTime().format(Utils.dateFormatter)

            if (!bindAlertIfNecessary(item)) {
                bindPlayAndInfo(item, isLoginRequired)
            }
        }

        private fun initListeners(loginRequired: Boolean) {
            // Subtract 1 from the bindingAdapterPosition, since we have a header.
            nameContainer.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) {
                    val resolvedPosition = positionResolver.resolve(it)

                    Triple(expandedItemId, data[resolvedPosition].id, resolvedPosition)
                }
                .autoDisposable(this)
                .subscribe { (previousItemId, newItemId, position) ->
                    if (newItemId == previousItemId) {
                        expandedItemId = null
                    } else {
                        expandedItemId = newItemId

                        if (previousItemId != null) {
                            notifyItemChanged(data.indexOfFirst { it.id == previousItemId })
                        }
                    }

                    notifyItemChanged(position)
                }

            uploaderText.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { data[positionResolver.resolve(it)] }
                .autoDisposable(this)
                .subscribe(uploaderClickSubject)

            translatorGroup.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { data[positionResolver.resolve(it)] }
                .autoDisposable(this)
                .subscribe(translatorGroupClickSubject)

            dismissAdAlert.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { positionResolver.resolve(it) }
                .doOnNext { storageHelper.lastAdAlertDate = Instant.now() }
                .doAfterNext { notifyItemChanged(it) }
                .autoDisposable(this)
                .subscribe()

            setAdInterval.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { positionResolver.resolve(it) }
                .doOnNext { storageHelper.lastAdAlertDate = Instant.now() }
                .doAfterNext { notifyItemChanged(it) }
                .autoDisposable(this)
                .subscribe(setAdIntervalClickSubject)

            play.clicks()
                .mapBindingAdapterPosition({ bindingAdapterPosition }) { data[positionResolver.resolve(it)] }
                .autoDisposable(this)
                .apply { if (loginRequired) subscribe(loginClickSubject) else subscribe(playClickSubject) }
        }

        private fun bindAlertIfNecessary(item: AnimeStream): Boolean {
            val threshold by unsafeLazy {
                storageHelper.lastAdAlertDate.plus(14, ChronoUnit.DAYS)
            }

            val shouldShowAdAlert = ProxerStreamResolver.supports(item.hosterName) &&
                threshold.isBefore(Instant.now()) &&
                storageHelper.profileSettings.adInterval <= 0

            return if (shouldShowAdAlert) {
                adAlert.isVisible = true
                play.isVisible = false
                info.isVisible = false

                true
            } else {
                adAlert.isVisible = false
                play.isVisible = true

                false
            }
        }

        private fun bindPlayAndInfo(item: AnimeStream, isLoginRequired: Boolean) {
            if (item.isSupported) {
                if (isLoginRequired) {
                    play.setText(R.string.error_action_login)
                    play.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)

                    info.isVisible = true
                    info.setText(R.string.fragment_anime_stream_login_required_warning)
                    info.setCompoundDrawablesWithIntrinsicBounds(
                        generateInfoDrawable(CommunityMaterial.Icon4.cmd_alert), null, null, null
                    )
                } else {
                    play.setText(R.string.fragment_anime_stream_play)
                    play.setCompoundDrawablesWithIntrinsicBounds(
                        generatePlayDrawable(), null, null, null
                    )

                    when {
                        item.isOfficial -> {
                            info.isVisible = true
                            info.setText(R.string.fragment_anime_stream_official_info)
                            info.setCompoundDrawablesWithIntrinsicBounds(
                                generateInfoDrawable(CommunityMaterial.Icon2.cmd_information), null, null, null
                            )
                        }
                        else -> info.isVisible = false
                    }
                }
            } else {
                info.isVisible = true
                info.setText(R.string.error_unsupported_hoster)
                info.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)

                play.isVisible = false
            }
        }

        private fun generatePlayDrawable(): IconicsDrawable {
            return IconicsDrawable(play.context)
                .icon(CommunityMaterial.Icon3.cmd_play)
                .sizeDp(28)
                .paddingDp(8)
                .colorAttr(play.context, R.attr.colorOnPrimary)
        }

        private fun generateInfoDrawable(icon: IIcon): IconicsDrawable {
            return IconicsDrawable(info.context, icon)
                .size(IconicsSize.dp(26))
                .iconColor(info.context)
        }
    }

    inner class MessageViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val message: BetterLinkTextView by bindView(R.id.message)

        fun bind(item: AnimeStream) {
            val messageText = (item.resolutionResult as StreamResolutionResult.Message).message

            message.linkClicks()
                .map { it.toHttpUrlOrNull().toOptional() }
                .filterSome()
                .autoDisposable(this)
                .subscribe(linkClickSubject)

            message.text = messageText
        }
    }
}
