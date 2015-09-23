package com.rubengees.proxerme.activity;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.Response;
import com.afollestad.bridge.ResponseValidator;
import com.rubengees.proxerme.R;
import com.rubengees.proxerme.connection.ProxerException;
import com.rubengees.proxerme.customtabs.CustomTabActivityHelper;
import com.rubengees.proxerme.customtabs.WebviewFallback;

import org.json.JSONObject;

import static com.rubengees.proxerme.connection.ProxerException.ErrorCodes.PROXER;
import static com.rubengees.proxerme.connection.ProxerException.ErrorCodes.UNKNOWN;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
@SuppressLint("Registered")
public class MainActivity extends AppCompatActivity {

    private static final String RESPONSE_ERROR = "error";
    private static final String RESPONSE_ERROR_MESSAGE = "msg";
    private static final String VALIDATOR_ID = "default-validator";

    private CustomTabActivityHelper customTabActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bridge.client().config().validators(new ResponseValidator() {
            @Override
            public boolean validate(@NonNull Response response) throws Exception {
                JSONObject json = response.asJsonObject();

                if (json.has(RESPONSE_ERROR)) {
                    if (json.getInt(RESPONSE_ERROR) == 0) {
                        return true;
                    }else{
                        if (json.has(RESPONSE_ERROR_MESSAGE)) {
                            throw new ProxerException(PROXER, json.getString(RESPONSE_ERROR_MESSAGE));
                        }else{
                            throw new ProxerException(UNKNOWN, "An unknown error occurred.");
                        }
                    }
                }else{
                    return false;
                }
            }

            @NonNull
            @Override
            public String id() {
                return VALIDATOR_ID;
            }
        });

        customTabActivityHelper = new CustomTabActivityHelper();
    }

    @Override
    protected void onStart() {
        super.onStart();

        customTabActivityHelper.bindCustomTabsService(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        customTabActivityHelper.unbindCustomTabsService(this);
        Bridge.cleanup();
    }

    public void setLikelyUrl(String url) {
        customTabActivityHelper.mayLaunchUrl(Uri.parse(url), null, null);
    }

    public void showPage(String url) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder(customTabActivityHelper
                .getSession()).setToolbarColor(ContextCompat.getColor(this, R.color.primary))
                .build();

        CustomTabActivityHelper.openCustomTab(
                this, customTabsIntent, Uri.parse(url), new WebviewFallback());
    }
}
