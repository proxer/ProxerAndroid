package me.proxer.app.chat.create

import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.chat.Participant
import me.proxer.app.chat.create.CreateChatParticipantAdapter.ViewHolder
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class CreateChatParticipantAdapter(savedInstanceState: Bundle?) : BaseAdapter<Participant, ViewHolder>() {

    private companion object {
        private const val LIST_STATE = "create_chat_participant_list"
    }

    var glide: GlideRequests? = null
    val removalSubject: PublishSubject<Participant> = PublishSubject.create()

    val participants: List<Participant>
        get() = ArrayList(data)

    init {
        data = savedInstanceState?.getParcelableArrayList(LIST_STATE) ?: emptyList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_participant, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    override fun saveInstanceState(outState: Bundle) = outState.putParcelableArrayList(LIST_STATE, ArrayList(data))

    fun add(participant: Participant) {
        data += participant

        notifyItemInserted(itemCount - 1)
    }

    fun contains(username: String) = data.find { it.username.equals(username, ignoreCase = true) } != null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val image: ImageView by bindView(R.id.image)
        internal val username: TextView by bindView(R.id.username)
        internal val delete: ImageButton by bindView(R.id.delete)

        init {
            delete.setIconicsImage(CommunityMaterial.Icon.cmd_close, 48, 16)

            delete.setOnClickListener {
                withSafeAdapterPosition(this) {
                    data[it].let { removedParticipant ->
                        data -= removedParticipant

                        notifyItemRemoved(it)

                        removalSubject.onNext(removedParticipant)
                    }
                }
            }
        }

        fun bind(item: Participant) {
            username.text = item.username

            if (item.image.isBlank()) {
                image.setIconicsImage(CommunityMaterial.Icon.cmd_account, 96, 16, R.color.colorAccent)
            } else {
                glide?.load(ProxerUrls.userImage(item.image).toString())
                        ?.transition(DrawableTransitionOptions.withCrossFade())
                        ?.circleCrop()
                        ?.format(when (DeviceUtils.shouldShowHighQualityImages(image.context)) {
                            true -> DecodeFormat.PREFER_ARGB_8888
                            false -> DecodeFormat.PREFER_RGB_565
                        })
                        ?.into(image)
            }
        }
    }
}
