package com.proxerme.app.helper

import android.app.Activity
import com.proxerme.app.R
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.entity.Option
import com.rubengees.introduction.entity.Slide

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class IntroductionHelper {

    constructor(activity: Activity) {
        IntroductionBuilder(activity)
                .withSlides(generateSlides(activity))
                .withSkipEnabled(R.string.introduction_skip)
                .introduceMyself()
    }

    private fun generateSlides(activity: Activity): List<Slide> {
        return arrayListOf(
                Slide().withTitle(R.string.introduction_welcome_title)
                        .withColorResource(R.color.colorPrimary)
                        .withImageResource(R.drawable.ic_proxer)
                        .withDescription(R.string.introduction_welcome_description),
                Slide().withTitle(R.string.introduction_notifications_title)
                        .withColorResource(R.color.colorAccent)
                        .withImageResource(R.drawable.ic_notifications)
                        .withOption(Option(activity.getString(
                                R.string.introduction_notifications_description), false))
        )
    }
}
