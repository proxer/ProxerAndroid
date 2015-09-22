package com.rubengees.proxerme.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.Response;
import com.afollestad.bridge.ResponseValidator;
import com.rubengees.proxerme.connection.ProxerException;

import org.json.JSONObject;

import static com.rubengees.proxerme.connection.ProxerException.ErrorCodes.PROXER;
import static com.rubengees.proxerme.connection.ProxerException.ErrorCodes.UNKNOWN;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bridge.client().config().host("http://proxer.me").validators(new ResponseValidator() {
            @Override
            public boolean validate(@NonNull Response response) throws Exception {
                JSONObject json = response.asJsonObject();

                if(json.has(("error"))){
                    if(json.getInt("error") == 0){
                        return true;
                    }else{
                        if (json.has("msg")) {
                            throw new ProxerException(PROXER, json.getString("msg"));
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
                return "default-validator";
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        Bridge.cleanup();
    }


}
