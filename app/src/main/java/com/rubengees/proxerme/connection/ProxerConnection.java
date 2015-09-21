package com.rubengees.proxerme.connection;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.BridgeException;
import com.afollestad.bridge.Callback;
import com.afollestad.bridge.Request;
import com.afollestad.bridge.Response;
import com.rubengees.proxerme.entity.News;

import org.json.JSONException;

import java.util.List;

import static com.rubengees.proxerme.connection.ProxerException.ErrorCodes.*;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class ProxerConnection {

    public static void loadNews(@IntRange(from = 1) int page, @NonNull final ResultCallback<List<News>> callback) {
        Bridge.client().get(UrlHolder.getNewsUrl(page)).request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException exception) {
                if (exception == null) {
                    try {
                        callback.onResult(ProxerParser.parseNewsJSON(response.asJsonObject()));
                    } catch (JSONException e) {
                        callback.onError(new ProxerException(UNPARSEABLE,
                                ErrorHandler.getMessageForErrorCode(UNPARSEABLE)));
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

    public interface ResultCallback<T> {
        void onResult(T result);

        void onError(ProxerException exception);
    }

}
