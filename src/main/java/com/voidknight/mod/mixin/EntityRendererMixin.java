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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mixin(PlayerEntityRenderer.class)
public abstract class EntityRendererMixin {

    private static final ConcurrentHashMap<String, MemberInfo> MEMBERS =
            new ConcurrentHashMap<>();

    private static volatile boolean syncStarted = false;

    private static void startSync() {
        if (syncStarted) return;

        synchronized (EntityRendererMixin.class) {
            if (syncStarted) return;
            syncStarted = true;

            ScheduledExecutorService scheduler =
                    Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "VK-Async-Sync");
                        t.setDaemon(true);
                        return t;
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
        HttpURLConnection conn = null;

        try {
            URL url = new URL(
                    "https://voidknight.onrender.com/api/tiers"
            );

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty(
                    "User-Agent",
                    "VoidKnightMod/1.0"
            );
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() != 200) {
                return;
            }

            try (InputStreamReader reader =
                         new InputStreamReader(conn.getInputStream())) {

                JsonElement root = JsonParser.parseReader(reader);
                Gson gson = new Gson();

                ConcurrentHashMap<String, MemberInfo> temp =
                        new ConcurrentHashMap<>();

                if (root != null && root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();

                    for (String key : obj.keySet()) {
                        MemberInfo member =
                                gson.fromJson(
                                        obj.get(key),
                                        MemberInfo.class
                                );

                        if (member != null) {
                            temp.put(
                                    key.toLowerCase().trim(),
                                    member
                            );
                        }
                    }
                }

                if (!temp.isEmpty()) {
                    MEMBERS.clear();
                    MEMBERS.putAll(temp);
                }
            }

        } catch (Throwable ignored) {
            // API unavailable/error: keep previous data
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @ModifyVariable(
            method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private Text modifyNametagText121(
            Text originalText,
            AbstractClientPlayerEntity player
    ) {
        return buildCustomText(originalText, player);
    }

    @ModifyVariable(
            method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private Text modifyNametagTextFallback(
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

        if (player == null || MEMBERS.isEmpty()) {
            return originalText;
        }

        try {
            String cleanIGN =
                    player.getGameProfile().getName();

            if (cleanIGN == null || cleanIGN.isEmpty()) {
                return originalText;
            }

            MemberInfo member =
                    MEMBERS.get(cleanIGN.toLowerCase().trim());

            if (member == null) {
                return originalText;
            }

            String vkIconChar = "\uE001 ";
            String modeIconChar = "";

            String modeVal =
                    member.mode != null
                            ? member.mode
                            : member.role;

            if (modeVal != null) {
                String m = modeVal.toLowerCase();

                if (m.contains("cpvp") ||
                        m.contains("crystal")) {

                    modeIconChar = " \uE002";

                } else if (m.contains("spvp") ||
                        m.contains("sword")) {

                    modeIconChar = " \uE003";

                } else if (m.contains("build")) {

                    modeIconChar = " \uE004";

                } else if (m.contains("grind")) {

                    modeIconChar = " \uE005";
                }
            }

            String tierText =
                    (member.tier != null &&
                     !member.tier.isEmpty())
                            ? " §e[" + member.tier + "]"
                            : "";

            MutableText newText =
                    Text.literal(vkIconChar);

            newText.append(originalText);

            newText.append(
                    Text.literal(
                            tierText + modeIconChar
                    )
            );

            return newText;

        } catch (Throwable ignored) {
            return originalText;
        }
    }
}
