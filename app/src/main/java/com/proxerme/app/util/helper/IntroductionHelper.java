package com.proxerme.app.util.helper;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.proxerme.app.R;
import com.rubengees.introduction.IntroductionBuilder;
import com.rubengees.introduction.IntroductionConfiguration;
import com.rubengees.introduction.entity.Option;
import com.rubengees.introduction.entity.Slide;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class IntroductionHelper {

    public static void build(@NonNull Activity activity) {
        new IntroductionBuilder(activity).withSlides(generateSlides(activity))
                .withOnSlideListener(new IntroductionConfiguration.OnSlideListener() {
                    @Override
                    protected void onSlideInit(int position, @NonNull TextView title,
                                               @NonNull ImageView image,
                                               @NonNull TextView description) {
                        switch (position) {
                            case 0:
                                Glide.with(image.getContext())
                                        .load(R.drawable.ic_introduction_proxer).into(image);
                                break;
                            case 1:
                                Glide.with(image.getContext())
                                        .load(R.drawable.ic_introduction_notifications).into(image);
                                break;
                        }
                    }
                }).withSkipEnabled(R.string.introduction_skip).introduceMyself();
    }

    @NonNull
    private static List<Slide> generateSlides(@NonNull Activity activity) {
        List<Slide> slides = new ArrayList<>(2);

        slides.add(new Slide().withTitle(R.string.introduction_welcome_title)
                .withColorResource(R.color.colorPrimary)
                .withDescription(R.string.introduction_welcome_description));
        slides.add(new Slide().withTitle(R.string.introduction_notifications_title)
                .withColorResource(R.color.colorAccent)
                .withOption(new Option(activity.
                        getString(R.string.introduction_notifications_description), false)));

        return slides;
    }

}
