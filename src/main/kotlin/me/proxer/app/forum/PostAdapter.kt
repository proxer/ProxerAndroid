package me.proxer.app.forum

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.forum.PostAdapter.ViewHolder
import me.proxer.app.ui.view.bbcode.BBCodeView
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class PostAdapter : BaseAdapter<ParsedPost, ViewHolder>() {

    var glide: GlideRequests? = null
    val profileClickSubject: PublishSubject<Pair<ImageView, ParsedPost>> = PublishSubject.create()

    private var layoutManager: LayoutManager? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        layoutManager = recyclerView.layoutManager
    }

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)

        holder.post.destroy()
        holder.signature.destroy()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        layoutManager = null
        glide = null
    }

    inner class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {

        internal val userContainer by bindView<ViewGroup>(R.id.userContainer)
        internal val image by bindView<ImageView>(R.id.image)
        internal val user by bindView<TextView>(R.id.user)
        internal val post by bindView<BBCodeView>(R.id.post)
        internal val signatureDivider by bindView<View>(R.id.signatureDivider)
        internal val signature by bindView<BBCodeView>(R.id.signature)
        internal val date by bindView<TextView>(R.id.date)
        internal val thankYouIcon by bindView<ImageView>(R.id.thankYouIcon)
        internal val thankYou by bindView<TextView>(R.id.thankYou)

        init {
            userContainer.setOnClickListener {
                withSafeAdapterPosition(this) {
                    profileClickSubject.onNext(image to data[it])
                }
            }

            post.glide = glide
            post.enableEmotions = true
            post.heightChangedListener = {
                post.requestLayout()
                layoutManager?.requestSimpleAnimationsInNextLayout()
            }

            signature.glide = glide
            post.enableEmotions = true
            signature.heightChangedListener = {
                signature.requestLayout()
                layoutManager?.requestSimpleAnimationsInNextLayout()
            }

            thankYouIcon.setIconicsImage(CommunityMaterial.Icon.cmd_thumb_up, 32)
        }

        fun bind(item: ParsedPost) {
            ViewCompat.setTransitionName(image, "post_${item.id}")

            user.text = item.username
            date.text = item.date.convertToRelativeReadableTime(date.context)
            thankYou.text = item.thankYouAmount.toString()

            post.userId = item.userId
            post.setTree(item.parsedMessage)

            item.signature.let {
                signature.userId = item.userId

                if (it == null) {
                    signatureDivider.visibility = View.GONE
                    signature.visibility = View.GONE
                    signature.destroy()
                } else {
                    signatureDivider.visibility = View.VISIBLE
                    signature.visibility = View.VISIBLE
                    signature.setTree(it)
                }
            }

            bindImage(item)
        }

        private fun bindImage(item: ParsedPost) {
            if (item.image.isBlank()) {
                image.setIconicsImage(CommunityMaterial.Icon.cmd_account, 32, 4, R.color.colorAccent)
            } else {
                glide?.load(ProxerUrls.userImage(item.image).toString())
                    ?.transition(DrawableTransitionOptions.withCrossFade())
                    ?.circleCrop()
                    ?.into(image)
            }
        }
    }
}
