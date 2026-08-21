package com.voidknight.mod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TierManager {

    private static final String API_URL =
            "https://voidknight.onrender.com/api/tiers";

    private static final long REFRESH_TIME_MS = 10_000L;

    private static final Gson GSON = new Gson();

    private static volatile Map<String, MemberInfo> members =
            Collections.emptyMap();

    private static volatile long lastFetch = 0L;
    private static volatile boolean fetching = false;

    private TierManager() {
    }

    public static MemberInfo getMember(String playerName) {
        refreshIfNeeded();
        return members.get(playerName);
    }

    public static void refreshIfNeeded() {
        long now = System.currentTimeMillis();

        if (fetching || now - lastFetch < REFRESH_TIME_MS) {
            return;
        }

        fetching = true;

        Thread thread = new Thread(() -> {
            try {
                HttpURLConnection connection =
                        (HttpURLConnection) URI.create(API_URL).toURL().openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5_000);
                connection.setReadTimeout(5_000);
                connection.setRequestProperty("Accept", "application/json");

                try (InputStreamReader reader = new InputStreamReader(
                        connection.getInputStream(),
                        StandardCharsets.UTF_8
                )) {
                    Map<String, MemberInfo> fetched = GSON.fromJson(
                            reader,
                            new TypeToken<Map<String, MemberInfo>>() {}.getType()
                    );

                    if (fetched != null) {
                        members = Collections.unmodifiableMap(
                                new HashMap<>(fetched)
                        );
                    } else {
                        members = Collections.emptyMap();
                    }

                    lastFetch = System.currentTimeMillis();

                    System.out.println(
                            "[VoidKnight] API updated: " + members.size() + " players"
                    );
                }

                connection.disconnect();

            } catch (Exception exception) {
                System.out.println(
                        "[VoidKnight] Could not fetch tiers: " +
                                exception.getMessage()
                );
            } finally {
                fetching = false;
            }
        }, "VoidKnight-API-Fetcher");

        thread.setDaemon(true);
        thread.start();
    }
}
