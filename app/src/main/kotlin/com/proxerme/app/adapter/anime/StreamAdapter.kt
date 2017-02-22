package com.proxerme.app.adapter.anime

import android.support.v4.util.LongSparseArray
import android.support.v7.widget.RecyclerView
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
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.anime.entity.Stream
import com.proxerme.library.info.ProxerUrlHolder
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class StreamAdapter(private val getRealPosition: (Int) -> Int) : PagingAdapter<Stream>() {

    private companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        private const val ICON_SIZE = 28
        private const val ICON_PADDING = 8
    }

    var callback: StreamAdapterCallback? = null

    private val expanded = LongSparseArray<Boolean>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            PagingViewHolder<Stream> {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_stream,
                parent, false))
    }

    override fun removeCallback() {
        callback = null
    }

    inner class ViewHolder(itemView: View) :
            PagingAdapter.PagingViewHolder<Stream>(itemView) {

        private val nameContainer: ViewGroup by bindView(R.id.nameContainer)
        private val name: TextView by bindView(R.id.name)
        private val image: ImageView by bindView(R.id.image)

        private val uploadInfoContainer: ViewGroup by bindView(R.id.uploadInfoContainer)
        private val uploaderText: TextView by bindView(R.id.uploader)
        private val translatorGroup: TextView by bindView(R.id.translatorGroup)
        private val dateText: TextView by bindView(R.id.date)

        private val watchButton: Button by bindView(R.id.play)

        init {
            nameContainer.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val number = list[getRealPosition(adapterPosition)].id.toLong()

                    if (expanded.get(number, false)) {
                        expanded.delete(number)
                    } else {
                        expanded.put(number, true)
                    }

                    notifyItemChanged(getRealPosition(adapterPosition))
                }
            }

            uploaderText.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onUploaderClick(list[getRealPosition(adapterPosition)])
                }
            }

            translatorGroup.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onTranslatorGroupClick(list[getRealPosition(adapterPosition)])
                }
            }

            watchButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onWatchClick(list[getRealPosition(adapterPosition)])
                }
            }

            watchButton.setCompoundDrawablesWithIntrinsicBounds(IconicsDrawable(watchButton.context)
                    .icon(CommunityMaterial.Icon.cmd_play)
                    .sizeDp(ICON_SIZE)
                    .paddingDp(ICON_PADDING)
                    .colorRes(android.R.color.white), null, null, null)
        }

        override fun bind(item: Stream) {
            name.text = item.hosterName
            uploaderText.text = item.uploader
            translatorGroup.text = item.translatorGroup ?:
                    translatorGroup.context.getString(R.string.fragment_anime_empty_subgoup)

            dateText.text = DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(item.time), ZoneId.systemDefault()))

            if (expanded.get(item.id.toLong(), false)) {
                uploadInfoContainer.visibility = View.VISIBLE
            } else {
                uploadInfoContainer.visibility = View.GONE
            }

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getHosterImageUrl(item.imageId).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    abstract class StreamAdapterCallback {
        open fun onUploaderClick(item: Stream) {

        }

        open fun onTranslatorGroupClick(item: Stream) {

        }

        open fun onWatchClick(item: Stream) {

        }
    }

}
