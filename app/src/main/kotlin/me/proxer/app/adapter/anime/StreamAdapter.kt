package me.proxer.app.adapter.anime

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.util.ParcelableStringBooleanMap
import me.proxer.app.util.TimeUtils
import me.proxer.app.util.extension.bindView
import me.proxer.library.entitiy.anime.Stream
import me.proxer.library.util.ProxerUrls
import org.threeten.bp.format.DateTimeFormatter

/**
 * @author Ruben Gees
 */
class StreamAdapter(savedInstanceState: Bundle?) : PagingAdapter<Stream>() {

    private companion object {
        private const val EXPANDED_STATE = "stream_expanded"
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }

    var callback: StreamAdapterCallback? = null

    private val expanded: ParcelableStringBooleanMap

    init {
        expanded = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<Stream> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_stream, parent, false))
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(EXPANDED_STATE, expanded)
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<Stream>(itemView) {

        private val nameContainer: ViewGroup by bindView(R.id.nameContainer)
        private val name: TextView by bindView(R.id.name)
        private val image: ImageView by bindView(R.id.image)

        private val uploadInfoContainer: ViewGroup by bindView(R.id.uploadInfoContainer)
        private val uploaderText: TextView by bindView(R.id.uploader)
        private val translatorGroup: TextView by bindView(R.id.translatorGroup)
        private val dateText: TextView by bindView(R.id.date)

        private val play: Button by bindView(R.id.play)

        init {
            nameContainer.setOnClickListener {
                withSafeAdapterPosition {
                    val id = list[it].id

                    if (expanded.getOrDefault(id, false)) {
                        expanded.remove(id)
                    } else {
                        expanded.put(id, true)
                    }

                    notifyItemChanged(it)
                }
            }

            uploaderText.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onUploaderClick(list[it])
                }
            }

            translatorGroup.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onTranslatorGroupClick(list[it])
                }
            }

            play.setOnClickListener {
                withSafeAdapterPosition {
                    callback?.onWatchClick(list[it])
                }
            }

            play.setCompoundDrawablesWithIntrinsicBounds(IconicsDrawable(play.context)
                    .icon(CommunityMaterial.Icon.cmd_play)
                    .sizeDp(28)
                    .paddingDp(8)
                    .colorRes(android.R.color.white), null, null, null)
        }

        override fun bind(item: Stream) {
            name.text = item.hosterName

            Glide.with(image.context)
                    .load(ProxerUrls.hosterImage(item.image).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)

            if (expanded.getOrDefault(item.id, false)) {
                uploadInfoContainer.visibility = View.VISIBLE
            } else {
                uploadInfoContainer.visibility = View.GONE

                return
            }

            uploaderText.text = item.uploaderName
            translatorGroup.text = item.translatorGroupName ?:
                    translatorGroup.context.getString(R.string.fragment_anime_empty_subgroup)

            dateText.text = DATE_TIME_FORMATTER.format(TimeUtils.convertToDateTime(item.date))
        }
    }

    interface StreamAdapterCallback {
        fun onUploaderClick(item: Stream) {

        }

        fun onTranslatorGroupClick(item: Stream) {

        }

        fun onWatchClick(item: Stream) {

        }
    }
}
