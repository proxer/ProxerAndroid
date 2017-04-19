package me.proxer.app.helper

import android.app.Activity
import android.content.Context
import com.rubengees.introduction.IntroductionBuilder
import com.rubengees.introduction.Option
import com.rubengees.introduction.Slide
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
                        .withDescription(R.string.introduction_welcome_description),
                Slide().withTitle(R.string.introduction_notifications_title)
                        .withColorResource(R.color.colorAccent)
                        .withImage(R.drawable.ic_notifications)
                        .withOption(Option(context.getString(R.string.introduction_notifications_description), false))
        )
    }

    init {
        IntroductionBuilder(activity)
                .withSlides(generateSlides(activity))
                .withSkipEnabled(R.string.introduction_skip)
                .introduceMyself()
    }
}
