package me.proxer.app.helper

import android.app.Activity
import android.content.Context
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.entity.Slide
import me.proxer.app.R

/**
 * @author Ruben Gees
 */
class IntroductionHelper(activity: Activity) {

    private fun generateSlides(context: Context): List<Slide> {
        return arrayListOf(
                Slide().withTitle(R.string.introduction_welcome_title)
                        .withColorResource(R.color.primary)
                        .withImage(R.drawable.ic_proxer)
                        .withDescription(R.string.introduction_welcome_description)
        )
    }

    init {
        IntroductionBuilder(activity)
                .withSlides(generateSlides(activity))
                .withSkipEnabled(R.string.introduction_skip)
                .introduceMyself()
    }
}
