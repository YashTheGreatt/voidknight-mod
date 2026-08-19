package com.voidknight.mod;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataManager {
    private static final String API_URL = "https://voidknight.onrender.com/api/tiers";
    private static final Map<String, MemberData> members = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void startAutoSync() {
        scheduler.scheduleAtFixedRate(DataManager::fetchMembers, 0, 30, TimeUnit.SECONDS);
    }

    public static void fetchMembers() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "VoidKnightMod/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                JsonElement jsonElement = JsonParser.parseReader(reader);
                reader.close();

                Gson gson = new Gson();
                Map<String, MemberData> tempMap = new HashMap<>();

                if (jsonElement.isJsonArray()) {
                    JsonArray array = jsonElement.getAsJsonArray();
                    for (JsonElement elem : array) {
                        MemberData member = gson.fromJson(elem, MemberData.class);
                        if (member != null && !member.getIgn().isEmpty()) {
                            tempMap.put(member.getIgn().toLowerCase().trim(), member);
                        }
                    }
                } else if (jsonElement.isJsonObject()) {
                    JsonObject obj = jsonElement.getAsJsonObject();
                    for (String key : obj.keySet()) {
                        MemberData member = gson.fromJson(obj.get(key), MemberData.class);
                        tempMap.put(key.toLowerCase().trim(), member);
                    }
                }

                synchronized (members) {
                    members.clear();
                    members.putAll(tempMap);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static MemberData getMember(String ign) {
        if (ign == null) return null;
        synchronized (members) {
            return members.get(ign.toLowerCase().trim());
        }
    }
}
