package com.voidknight.mod.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.voidknight.mod.MemberInfo;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
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
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() != 200) {
                return;
            }

            try (InputStreamReader reader =
                         new InputStreamReader(conn.getInputStream())) {

                JsonElement root = JsonParser.parseReader(reader);

                if (root == null || !root.isJsonObject()) {
                    return;
                }

                Gson gson = new Gson();
                ConcurrentHashMap<String, MemberInfo> temp =
                        new ConcurrentHashMap<>();

                JsonObject obj = root.getAsJsonObject();

                for (String key : obj.keySet()) {
                    MemberInfo member =
                            gson.fromJson(
                                    obj.get(key),
                                    MemberInfo.class
                            );

                    if (member != null) {
                        temp.put(
                                key.toLowerCase(Locale.ROOT).trim(),
                                member
                        );
                    }
                }

                // API ka latest data replace karo.
                MEMBERS.clear();
                MEMBERS.putAll(temp);
            }

        } catch (Throwable ignored) {
            // API fail hone par game crash nahi hoga.
            // Previous data rahega.
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
        startSync();

        if (player == null) {
            return originalText;
        }

        try {
            // Server/LuckPerms ka MEMBER text use nahi karenge.
            // Direct Minecraft IGN lenge.
            String ign = player.getGameProfile().getName();

            if (ign == null || ign.isBlank()) {
                return originalText;
            }

            MemberInfo member = MEMBERS.get(
                    ign.toLowerCase(Locale.ROOT).trim()
            );

            // API mein member nahi hai → normal server nametag.
            if (member == null) {
                return originalText;
            }

            String role = member.role;

            if (role == null || role.isBlank()) {
                role = member.mode;
            }

            if (role == null || role.isBlank()) {
                return Text.literal(ign);
            }

            role = role.toUpperCase(Locale.ROOT).trim();

            // IMPORTANT:
            // \uE001 = VKVV logo
            // \uE002 = Crystal / CPVPER
            // \uE003 = Sword
            // \uE004 = Builder
            // \uE005 = Grinder
            String vkLogo = "\uE001 ";
            String roleIcon;

            switch (role) {
                case "CPVPER":
                case "CPVP":
                case "CRYSTAL":
                    role = "CPVPER";
                    roleIcon = "\uE002";
                    break;

                case "SWORD":
                case "SWORDPVP":
                    role = "SWORD";
                    roleIcon = "\uE003";
                    break;

                case "BUILDER":
                    roleIcon = "\uE004";
                    break;

                case "GRINDER":
                    roleIcon = "\uE005";
                    break;

                default:
                    return Text.literal(
                            vkLogo + ign + " " + role
                    );
            }

            return Text.literal(
                    vkLogo
                            + ign
                            + " "
                            + roleIcon
                            + " "
                            + role
            );

        } catch (Throwable ignored) {
            return originalText;
        }
    }
}
