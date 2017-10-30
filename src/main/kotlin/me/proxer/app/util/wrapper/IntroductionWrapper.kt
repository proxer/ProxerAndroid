package me.proxer.app.util.wrapper

import android.app.Activity
import android.content.Context
import android.support.v7.content.res.AppCompatResources
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.Option
import com.rubengees.introduction.Slide
import com.rubengees.introduction.interfaces.OnSlideListener
import me.proxer.app.R
import me.proxer.app.util.extension.setIconicsImage

/**
 * @author Ruben Gees
 */
object IntroductionWrapper {

    fun introduce(activity: Activity) = IntroductionBuilder(activity)
            .withSlides(generateSlides(activity))
            .withSkipEnabled(R.string.introduction_skip)
            .withOnSlideListener(object : OnSlideListener() {
                override fun onSlideInit(position: Int, title: TextView, image: ImageView, description: TextView) {
                    when (position) {
                        0 -> image.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_proxer))
                        1 -> image.setIconicsImage(CommunityMaterial.Icon.cmd_bell_outline, 256, 16,
                                android.R.color.white)
                    }
                }
            })
            .introduceMyself()

    private fun generateSlides(context: Context) = arrayListOf(
            Slide().withTitle(R.string.introduction_welcome_title)
                    .withColorResource(R.color.primary)
                    .withDescription(R.string.introduction_welcome_description),
            Slide().withTitle(R.string.introduction_notifications_title)
                    .withColorResource(R.color.colorAccent)
                    .withOption(Option(context.getString(R.string.introduction_notifications_description), true))
    )
}
