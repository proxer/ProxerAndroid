package com.rubengees.proxerme.connection;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Callback;
import com.afollestad.bridge.Request;
import com.afollestad.bridge.Response;
import com.afollestad.bridge.ResponseValidator;
import com.rubengees.proxerme.entity.News;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.rubengees.proxerme.connection.ErrorHandler.ErrorCodes.PROXER;
import static com.rubengees.proxerme.connection.ErrorHandler.ErrorCodes.UNKNOWN;
import static com.rubengees.proxerme.connection.ErrorHandler.ErrorCodes.UNPARSEABLE;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class ProxerConnection {
    private static final String RESPONSE_ERROR = "error";
    private static final String RESPONSE_ERROR_MESSAGE = "msg";
    private static final String VALIDATOR_ID = "default-validator";

    public static void loadNews(@IntRange(from = 1) int page, @NonNull final ResultCallback<List<News>> callback) {
        Bridge.client().get(UrlHolder.getNewsUrl(page)).request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException exception) {
                if (exception == null) {
                    try {
                        callback.onResult(ProxerParser.parseNewsJSON(response.asJsonObject()));
                    } catch (JSONException e) {
                        callback.onError(new ProxerException(UNPARSEABLE));
                    } catch (BridgeException e) {
                        callback.onError(ErrorHandler.handleException(e));
                    }
                } else {
                    if (exception.reason() != BridgeException.REASON_REQUEST_CANCELLED) {
                        callback.onError(ErrorHandler.handleException(exception));
                    }
                }
            }
        });
    }

    public static List<News> loadNewsSync(@IntRange(from = 1) int page) throws BridgeException,
            JSONException {
        JSONObject result = Bridge.client().get(UrlHolder.getNewsUrl(page)).asJsonObject();

        return ProxerParser.parseNewsJSON(result);
    }

    public static void initBridge() {
        Bridge.client().config().validators(new ResponseValidator() {
            @Override
            public boolean validate(@NonNull Response response) throws Exception {
                JSONObject json = response.asJsonObject();

                if (json.has(RESPONSE_ERROR)) {
                    if (json.getInt(RESPONSE_ERROR) == 0) {
                        return true;
                    } else {
                        if (json.has(RESPONSE_ERROR_MESSAGE)) {
                            throw new ProxerException(PROXER, json.getString(RESPONSE_ERROR_MESSAGE));
                        } else {
                            throw new ProxerException(UNKNOWN, "An unknown error occurred.");
                        }
                    }
                } else {
                    return false;
                }
            }

            @NonNull
            @Override
            public String id() {
                return VALIDATOR_ID;
            }
        });
    }

    public interface ResultCallback<T> {
        void onResult(T result);

        void onError(ProxerException exception);
    }

}
