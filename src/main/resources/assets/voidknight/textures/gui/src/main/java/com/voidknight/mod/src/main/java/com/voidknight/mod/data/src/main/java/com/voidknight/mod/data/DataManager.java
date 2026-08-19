package com.voidknight.mod.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    public static final ConcurrentHashMap<String, MemberData> MEMBERS = new ConcurrentHashMap<>();
    private static final String API_URL = "https://voidknight.onrender.com/api/tiers";
    private static final Gson GSON = new Gson();

    public static void fetchClanData() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                Type type = new TypeToken<ConcurrentHashMap<String, MemberData>>() {}.getType();
                ConcurrentHashMap<String, MemberData> freshData = GSON.fromJson(reader, type);
                reader.close();

                if (freshData != null) {
                    MEMBERS.clear();
                    MEMBERS.putAll(freshData);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
