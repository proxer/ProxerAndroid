package com.proxerme.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.proxerme.app.R;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.News;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * An Activity which shows a image with an animation on Lollipop and higher.
 * It also has a transparent background.
 *
 * @author Ruben Gees
 */
public class NewsImageDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NEWS = "extra_news";

    @Bind(R.id.activity_news_image_detail_image)
    ImageView image;

    public static void navigateTo(@NonNull Activity context, @NonNull ImageView image, @NonNull News news) {
        Intent intent = new Intent(context, NewsImageDetailActivity.class);

        intent.putExtra(EXTRA_NEWS, news);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(context, image, image.getTransitionName());
            context.startActivity(intent, options.toBundle());
        } else {
            context.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_image_detail);

        ButterKnife.bind(this);

        News news = getIntent().getParcelableExtra(EXTRA_NEWS);

        supportPostponeEnterTransition();
        Glide.with(this).load(UrlHolder.getNewsImageUrl(news.getId(), news.getImageId()))
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model,
                                               Target<GlideDrawable> target,
                                               boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model,
                                                   Target<GlideDrawable> target,
                                                   boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        supportStartPostponedEnterTransition();
                        return false;
                    }
                })
                .into(image);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.activity_news_image_detail_root)
    void exit() {
        supportFinishAfterTransition();
    }
}
