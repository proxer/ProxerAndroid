package me.proxer.app.anime

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.anime.AnimeAdapter.ViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.Utils
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.convertToDateTime
import me.proxer.app.util.extension.defaultLoad
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class AnimeAdapter(savedInstanceState: Bundle?) : BaseAdapter<AnimeStream, ViewHolder>() {

    private companion object {
        private const val EXPANDED_STATE = "anime_stream_expanded"
    }

    var glide: GlideRequests? = null
    val uploaderClickSubject: PublishSubject<AnimeStream> = PublishSubject.create()
    val translatorGroupClickSubject: PublishSubject<AnimeStream> = PublishSubject.create()
    val playClickSubject: PublishSubject<AnimeStream> = PublishSubject.create()

    private val expansionMap: ParcelableStringBooleanMap

    init {
        expansionMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
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

    override fun saveInstanceState(outState: Bundle) = outState.putParcelable(EXPANDED_STATE, expansionMap)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val nameContainer: ViewGroup by bindView(R.id.nameContainer)
        internal val name: TextView by bindView(R.id.name)
        internal val image: ImageView by bindView(R.id.image)

        internal val uploadInfoContainer: ViewGroup by bindView(R.id.uploadInfoContainer)
        internal val uploaderText: TextView by bindView(R.id.uploader)
        internal val translatorGroup: TextView by bindView(R.id.translatorGroup)
        internal val dateText: TextView by bindView(R.id.date)

        internal val play: Button by bindView(R.id.play)
        internal val unsupported: TextView by bindView(R.id.unsupported)

        init {
            nameContainer.setOnClickListener {
                withSafeAdapterPosition(this) {
                    expansionMap.putOrRemove(data[it].id)

                    notifyItemChanged(it)
                }
            }

            uploaderText.setOnClickListener {
                withSafeAdapterPosition(this) {
                    uploaderClickSubject.onNext(data[it])
                }
            }

            translatorGroup.setOnClickListener {
                withSafeAdapterPosition(this) {
                    translatorGroupClickSubject.onNext(data[it])
                }
            }

            play.setOnClickListener {
                withSafeAdapterPosition(this) {
                    playClickSubject.onNext(data[it])
                }
            }

            play.setCompoundDrawablesWithIntrinsicBounds(IconicsDrawable(play.context)
                    .icon(CommunityMaterial.Icon.cmd_play)
                    .sizeDp(28)
                    .paddingDp(8)
                    .colorRes(android.R.color.white), null, null, null)
        }

        fun bind(item: AnimeStream) {
            name.text = item.hosterName

            glide?.defaultLoad(image, ProxerUrls.hosterImage(item.image))

            if (expansionMap.containsKey(item.id)) {
                uploadInfoContainer.visibility = View.VISIBLE
            } else {
                uploadInfoContainer.visibility = View.GONE

                return
            }

            uploaderText.text = item.uploaderName
            translatorGroup.text = item.translatorGroupName
                    ?: translatorGroup.context.getString(R.string.fragment_anime_empty_subgroup)

            dateText.text = Utils.dateFormatter.format(item.date.convertToDateTime())

            play.visibility = if (item.isSupported) View.VISIBLE else View.GONE
            unsupported.visibility = if (item.isSupported) View.GONE else View.VISIBLE
        }
    }
}
