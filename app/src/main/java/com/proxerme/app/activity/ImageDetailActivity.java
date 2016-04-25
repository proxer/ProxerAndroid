package com.proxerme.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.proxerme.app.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.ActivityManager.TaskDescription;

/**
 * An Activity which shows a image with an animation on Lollipop and higher.
 * It also has a transparent background.
 *
 * @author Ruben Gees
 */
public class ImageDetailActivity extends AppCompatActivity {

    private static final String EXTRA_URL = "extra_url";

    @BindView(R.id.activity_news_image_detail_image)
    ImageView image;

    public static void navigateTo(@NonNull Activity context, @NonNull ImageView image,
                                  @NonNull String url) {
            Intent intent = new Intent(context, ImageDetailActivity.class);

            intent.putExtra(EXTRA_URL, url);

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
        setContentView(R.layout.activity_image_detail);

        ButterKnife.bind(this);
        styleRecents();

        String url = getIntent().getStringExtra(EXTRA_URL);

        supportPostponeEnterTransition();
        Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.SOURCE)
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

    private void styleRecents() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            TaskDescription taskDesc = new TaskDescription(getString(R.string.app_name), bm,
                    ContextCompat.getColor(this, R.color.colorPrimary));
            setTaskDescription(taskDesc);
        }
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
