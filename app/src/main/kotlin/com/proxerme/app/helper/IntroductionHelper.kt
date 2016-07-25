package com.proxerme.app.helper

import android.app.Activity
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.proxerme.app.R
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.IntroductionConfiguration
import com.rubengees.introduction.entity.Option
import com.rubengees.introduction.entity.Slide

/**
 * TODO: Describe Class

 * @author Ruben Gees
 */
class IntroductionHelper {

    constructor(activity: Activity) {
        IntroductionBuilder(activity)
                .withSlides(generateSlides(activity))
                .withOnSlideListener(object : IntroductionConfiguration.OnSlideListener() {
                    override fun onSlideInit(position: Int, title: TextView,
                                             image: ImageView,
                                             description: TextView) {
                        when (position) {
                            0 -> Glide.with(image.context)
                                    .load(R.drawable.ic_introduction_proxer)
                                    .into(image)
                            1 -> Glide.with(image.context)
                                    .load(R.drawable.ic_introduction_notifications)
                                    .into(image)
                        }
                    }
                }).withSkipEnabled(R.string.introduction_skip).introduceMyself()
    }

    private fun generateSlides(activity: Activity): List<Slide> {
        return arrayListOf(
                Slide()
                        .withTitle(R.string.introduction_welcome_title)
                        .withColorResource(R.color.colorPrimary)
                        .withDescription(R.string.introduction_welcome_description),
                Slide()
                        .withTitle(R.string.introduction_notifications_title)
                        .withColorResource(R.color.colorAccent)
                        .withOption(Option(activity.getString(
                                R.string.introduction_notifications_description), false))

        )
    }

}
