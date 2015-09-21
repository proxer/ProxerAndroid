package com.rubengees.proxerme.connection;

import android.support.annotation.NonNull;

import com.rubengees.proxerme.entity.News;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
public class ProxerParser {

    @NonNull
    public static List<News> parseNewsJSON(@NonNull JSONObject object) throws JSONException {
        JSONArray newsArray = object.getJSONArray("notifications");
        List<News> result = new ArrayList<>(newsArray.length());

        for (int i = 0; i < newsArray.length(); i++) {
            JSONObject newsObject = newsArray.getJSONObject(i);

            result.add(new News(newsObject.getInt("nid"), newsObject.getLong("time"),
                    newsObject.getString("description"), newsObject.getLong("image_id"),
                    newsObject.getString("subject"), newsObject.getInt("hits"),
                    newsObject.getInt("thread"), newsObject.getInt("uid"),
                    newsObject.getString("uname"), newsObject.getInt("posts"),
                    newsObject.getInt("catid"), newsObject.getString("catname")));
        }

        return result;
    }

}
