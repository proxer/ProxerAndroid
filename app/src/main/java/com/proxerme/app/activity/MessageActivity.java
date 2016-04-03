package com.proxerme.app.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.proxerme.app.fragment.MessagesFragment;
import com.proxerme.library.entity.Conference;

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
public class MessageActivity extends MainActivity {

    private static final String ARGUMENT_CONFERENCE = "conference";

    public static void navigateTo(@NonNull Activity context, @NonNull Conference conference) {
        Intent intent = new Intent(context, MessageActivity.class);

        intent.putExtra(ARGUMENT_CONFERENCE, conference);

        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Conference conference = getIntent().getParcelableExtra(ARGUMENT_CONFERENCE);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            setFragment(MessagesFragment.newInstance(conference.getId()), conference.getTopic());
        }
    }
}
