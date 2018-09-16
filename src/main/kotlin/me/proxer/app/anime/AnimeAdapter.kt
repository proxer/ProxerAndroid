package me.proxer.app.anime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.anime.AnimeAdapter.ViewHolder
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.Utils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.iconColor
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class AnimeAdapter(
    savedInstanceState: Bundle?,
    private val storageHelper: StorageHelper
) : BaseAdapter<AnimeStream, ViewHolder>() {

    private companion object {
        private const val EXPANDED_ITEM_STATE = "anime_stream_expanded_id"
    }

    var glide: GlideRequests? = null
    val uploaderClickSubject: PublishSubject<AnimeStream> = PublishSubject.create()
    val translatorGroupClickSubject: PublishSubject<AnimeStream> = PublishSubject.create()
    val playClickSubject: PublishSubject<AnimeStream> = PublishSubject.create()
    val loginClickSubject: PublishSubject<AnimeStream> = PublishSubject.create()

    private var expandedItemId: String?

    init {
        expandedItemId = when (savedInstanceState) {
            null -> null
            else -> savedInstanceState.getString(EXPANDED_ITEM_STATE)
        }

        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = data[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_stream, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    override fun saveInstanceState(outState: Bundle) = outState.putString(EXPANDED_ITEM_STATE, expandedItemId)

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val nameContainer: ViewGroup by bindView(R.id.nameContainer)
        internal val name: TextView by bindView(R.id.name)
        internal val image: ImageView by bindView(R.id.image)

        internal val uploadInfoContainer: ViewGroup by bindView(R.id.uploadInfoContainer)
        internal val uploaderText: TextView by bindView(R.id.uploader)
        internal val translatorGroup: TextView by bindView(R.id.translatorGroup)
        internal val dateText: TextView by bindView(R.id.date)

        internal val info: TextView by bindView(R.id.info)
        internal val play: Button by bindView(R.id.play)
        internal val unsupported: TextView by bindView(R.id.unsupported)

        fun bind(item: AnimeStream) {
            val isLoginRequired = !item.isOfficial && !storageHelper.isLoggedIn

            initListeners(isLoginRequired)

            name.text = item.hosterName

            glide?.defaultLoad(image, ProxerUrls.hosterImage(item.image))

            if (expandedItemId == item.id) {
                uploadInfoContainer.isVisible = true
            } else {
                uploadInfoContainer.isGone = true

                return
            }

            uploaderText.text = item.uploaderName
            translatorGroup.text = item.translatorGroupName
                ?: translatorGroup.context.getString(R.string.fragment_anime_empty_subgroup)

            dateText.text = Utils.dateFormatter.format(item.date.convertToDateTime())

            bindInfoAndPlay(item, isLoginRequired)
        }

        private fun initListeners(loginRequired: Boolean) {
            // Subtract 1 from the adapterPosition, since we have a header.
            nameContainer.clicks()
                .mapAdapterPosition({ adapterPosition }) {
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
                .mapAdapterPosition({ adapterPosition }) { data[positionResolver.resolve(it)] }
                .autoDisposable(this)
                .subscribe(uploaderClickSubject)

            translatorGroup.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[positionResolver.resolve(it)] }
                .autoDisposable(this)
                .subscribe(translatorGroupClickSubject)

            play.clicks()
                .mapAdapterPosition({ adapterPosition }) { data[positionResolver.resolve(it)] }
                .autoDisposable(this)
                .apply { if (loginRequired) subscribe(loginClickSubject) else subscribe(playClickSubject) }
        }

        private fun bindInfoAndPlay(item: AnimeStream, isLoginRequired: Boolean) {
            if (item.isSupported) {
                play.isVisible = true
                unsupported.isGone = true

                if (isLoginRequired) {
                    play.setText(R.string.error_action_login)
                    play.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)

                    info.isVisible = true
                    info.setText(R.string.fragment_anime_stream_login_required_warning)
                    info.setCompoundDrawablesWithIntrinsicBounds(
                        generateInfoDrawable(CommunityMaterial.Icon.cmd_alert), null, null, null
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
                                generateInfoDrawable(CommunityMaterial.Icon.cmd_information), null, null, null
                            )
                        }
                        item.isInternalPlayerOnly -> {
                            info.isVisible = true
                            info.setText(R.string.fragment_anime_stream_only_internal_player_warning)
                            info.setCompoundDrawablesWithIntrinsicBounds(
                                generateInfoDrawable(CommunityMaterial.Icon.cmd_alert), null, null, null
                            )
                        }
                        else -> info.isGone = true
                    }
                }
            } else {
                play.isGone = true
                unsupported.isVisible = true
            }
        }

        private fun generatePlayDrawable(): IconicsDrawable {
            return IconicsDrawable(play.context)
                .icon(CommunityMaterial.Icon.cmd_play)
                .sizeDp(28)
                .paddingDp(8)
                .colorRes(android.R.color.white)
        }

        private fun generateInfoDrawable(icon: CommunityMaterial.Icon): IconicsDrawable {
            return IconicsDrawable(info.context)
                .icon(icon)
                .sizeDp(26)
                .iconColor(info.context)
        }
    }
}
