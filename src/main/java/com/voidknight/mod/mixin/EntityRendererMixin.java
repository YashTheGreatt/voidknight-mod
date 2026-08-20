package com.voidknight.mod.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.voidknight.mod.MemberInfo;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mixin(PlayerEntityRenderer.class)
public abstract class EntityRendererMixin {

    private static final String API_URL =
            "https://voidknight.onrender.com/api/tiers";

    private static final ConcurrentHashMap<String, MemberInfo> MEMBERS =
            new ConcurrentHashMap<>();

    private static volatile boolean syncStarted = false;

    private static void startSync() {
        if (syncStarted) {
            return;
        }

        synchronized (EntityRendererMixin.class) {
            if (syncStarted) {
                return;
            }

            syncStarted = true;

            ScheduledExecutorService scheduler =
                    Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread thread = new Thread(r, "VoidKnight-API-Sync");
                        thread.setDaemon(true);
                        return thread;
                    });

            scheduler.scheduleAtFixedRate(
                    EntityRendererMixin::fetchData,
                    0,
                    10,
                    TimeUnit.SECONDS
            );
        }
    }

    private static void fetchData() {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty(
                    "User-Agent",
                    "VoidKnight-Mod/1.0"
            );

            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                return;
            }

            try (
                    InputStreamReader reader =
                            new InputStreamReader(
                                    connection.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
            ) {
                JsonElement root = JsonParser.parseReader(reader);

                if (root == null || !root.isJsonObject()) {
                    return;
                }

                Gson gson = new Gson();
                JsonObject object = root.getAsJsonObject();

                ConcurrentHashMap<String, MemberInfo> newMembers =
                        new ConcurrentHashMap<>();

                for (String ign : object.keySet()) {
                    JsonElement value = object.get(ign);

                    if (value == null || !value.isJsonObject()) {
                        continue;
                    }

                    MemberInfo member =
                            gson.fromJson(value, MemberInfo.class);

                    if (member != null && ign != null) {
                        newMembers.put(
                                ign.toLowerCase().trim(),
                                member
                        );
                    }
                }

                MEMBERS.clear();
                MEMBERS.putAll(newMembers);
            }

        } catch (Exception ignored) {

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /*
     * Minecraft 1.21.x target.
     *
     * Purana fallback method hata diya hai,
     * isliye invalid target-method warning nahi aani chahiye.
     */
    @ModifyVariable(
            method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private Text voidknight$modifyNametag(
            Text originalText,
            AbstractClientPlayerEntity player
    ) {
        return buildCustomText(originalText, player);
    }

    private Text buildCustomText(
            Text originalText,
            AbstractClientPlayerEntity player
    ) {
        if (!syncStarted) {
            startSync();
        }

        if (player == null) {
            return originalText;
        }

        try {
            String ign = player.getGameProfile().getName();

            if (ign == null || ign.trim().isEmpty()) {
                return originalText;
            }

            MemberInfo member =
                    MEMBERS.get(ign.toLowerCase().trim());

            if (member == null) {
                return originalText;
            }

            String role = member.getRole();

            if (role == null || role.trim().isEmpty()) {
                return originalText;
            }

            role = role.trim();

            /*
             * VK logo character.
             * Yeh tumhare custom font glyph se match hona chahiye.
             */
            String vkIcon = "\uE001 ";

            /*
             * Role icons
             */
            String roleIcon = "";

            String lowerRole = role.toLowerCase();

            if (lowerRole.contains("cpvp")
                    || lowerRole.contains("crystal")) {

                roleIcon = " \uE002";

            } else if (lowerRole.contains("sword")) {

                roleIcon = " \uE003";

            } else if (lowerRole.contains("builder")
                    || lowerRole.contains("build")) {

                roleIcon = " \uE004";

            } else if (lowerRole.contains("grinder")
                    || lowerRole.contains("grind")) {

                roleIcon = " \uE005";
            }

            MutableText result = Text.literal(vkIcon);

            // Original Minecraft username/rank text
            result.append(originalText);

            // Role icon + role name
            result.append(
                    Text.literal(roleIcon + " [" + role + "]")
            );

            return result;

        } catch (Exception ignored) {
            return originalText;
        }
    }
}
