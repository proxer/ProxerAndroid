package com.proxerme.app.activity

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.widget.Toolbar
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.fragment.info.TranslatorGroupInfoFragment
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import org.jetbrains.anko.intentFor

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class TranslatorGroupActivity : MainActivity() {

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_IMAGE_ID = "extra_image_id"

        fun navigateTo(context: Activity, id: String, name: String? = null,
                       imageId: String? = null) {
            context.startActivity(context.intentFor<TranslatorGroupActivity>(
                    EXTRA_ID to id,
                    EXTRA_NAME to name,
                    EXTRA_IMAGE_ID to imageId)
            )
        }
    }

    private val id: String
        get() = intent.getStringExtra(EXTRA_ID)

    private var name: String?
        get() = intent.getStringExtra(EXTRA_NAME)
        set(value) {
            intent.putExtra(EXTRA_NAME, value)
        }

    private var imageId: String?
        get() = intent.getStringExtra(EXTRA_IMAGE_ID)
        set(value) {
            intent.putExtra(EXTRA_IMAGE_ID, value)
        }

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val collapsingToolbar: CollapsingToolbarLayout by bindView(R.id.collapsingToolbar)
    private val image: ImageView by bindView(R.id.image)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_translator_group)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        collapsingToolbar.isTitleEnabled = false
        title = name

        image.setOnClickListener {
            if (!imageId.isNullOrBlank()) {
                ImageDetailActivity.navigateTo(this@TranslatorGroupActivity, it as ImageView,
                        Utils.parseAndFixUrl(imageId!!))
            }
        }

        loadImage()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.container,
                    TranslatorGroupInfoFragment.newInstance(id)).commitNow()
        }
    }

    fun update(newName: String, newImageId: String) {
        name = newName
        title = newName

        if (imageId == null) {
            imageId = newImageId

            loadImage()
        }
    }

    private fun loadImage() {
        if (imageId.isNullOrBlank()) {
            image.setImageDrawable(IconicsDrawable(image.context)
                    .icon(CommunityMaterial.Icon.cmd_account_multiple)
                    .sizeDp(256)
                    .paddingDp(32)
                    .backgroundColorRes(R.color.colorPrimaryLight)
                    .colorRes(R.color.colorPrimary))
        } else {
            Glide.with(this).load(imageId)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onResourceReady(resource: GlideDrawable?, model: String?,
                                                     target: Target<GlideDrawable>?,
                                                     isFromMemoryCache: Boolean,
                                                     isFirstResource: Boolean): Boolean {
                            return false
                        }

                        override fun onException(e: Exception?, model: String?,
                                                 target: Target<GlideDrawable>?,
                                                 isFirstResource: Boolean): Boolean {
                            imageId = ""

                            loadImage()

                            return true
                        }

                    })
                    .into(image)
        }
    }
}