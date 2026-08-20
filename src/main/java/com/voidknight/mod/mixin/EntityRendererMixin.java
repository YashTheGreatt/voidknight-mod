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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mixin(PlayerEntityRenderer.class)
public abstract class EntityRendererMixin {

    private static final String API_URL =
            "https://voidknight.onrender.com/api/tiers";

    private static final Gson GSON = new Gson();

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

            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return;
            }

            try (
                    InputStreamReader reader = new InputStreamReader(
                            connection.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            ) {

                JsonElement root = JsonParser.parseReader(reader);

                if (root == null || !root.isJsonObject()) {
                    return;
                }

                JsonObject object = root.getAsJsonObject();

                ConcurrentHashMap<String, MemberInfo> newMembers =
                        new ConcurrentHashMap<>();

                for (Map.Entry<String, JsonElement> entry
                        : object.entrySet()) {

                    if (entry.getValue() == null
                            || !entry.getValue().isJsonObject()) {
                        continue;
                    }

                    MemberInfo member =
                            GSON.fromJson(
                                    entry.getValue(),
                                    MemberInfo.class
                            );

                    if (member != null) {
                        newMembers.put(
                                entry.getKey()
                                        .toLowerCase()
                                        .trim(),
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
     * IMPORTANT:
     * Old fallback with only "...;I)V" was removed because
     * your build log showed that method does not exist.
     */
    @ModifyVariable(
            method =
                    "renderLabelIfPresent("
                    + "Lnet/minecraft/client/network/AbstractClientPlayerEntity;"
                    + "Lnet/minecraft/text/Text;"
                    + "Lnet/minecraft/client/util/math/MatrixStack;"
                    + "Lnet/minecraft/client/render/VertexConsumerProvider;"
                    + "IF)V",
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

    private static Text buildCustomText(
            Text originalText,
            AbstractClientPlayerEntity player
    ) {

        startSync();

        if (originalText == null || player == null) {
            return originalText;
        }

        String ign;

        try {
            ign = player.getGameProfile().getName();
        } catch (Exception ignored) {
            return originalText;
        }

        if (ign == null || ign.isBlank()) {
            return originalText;
        }

        MemberInfo member =
                MEMBERS.get(ign.toLowerCase().trim());

        /*
         * Player website/API roster me nahi hai.
         * Normal nametag bilkul unchanged.
         */
        if (member == null) {
            return originalText;
        }

        /*
         * Font image characters
         *
         * E001 = VK logo
         * E002 = Crystal / CPvP
         * E003 = Sword
         * E004 = Builder
         * E005 = Grinder
         */
        String vkLogo = "\uE001 ";
        String roleIcon = "";
        String roleName = "";

        if (member.role != null) {

            String role =
                    member.role
                            .trim()
                            .toLowerCase();

            if (
                    role.contains("cpvp")
                            || role.contains("crystal")
            ) {
                roleIcon = " \uE002";
                roleName = " CPvPer";

            } else if (
                    role.contains("sword")
                            || role.contains("spvp")
            ) {
                roleIcon = " \uE003";
                roleName = " Sword";

            } else if (role.contains("builder")) {
                roleIcon = " \uE004";
                roleName = " Builder";

            } else if (role.contains("grinder")) {
                roleIcon = " \uE005";
                roleName = " Grinder";

            } else {
                roleName = " " + member.role;
            }
        }

        MutableText result = Text.literal(vkLogo);

        /*
         * VK logo + username
         */
        result.append(originalText);

        /*
         * Role icon + role
         */
        if (!roleIcon.isEmpty()) {
            result.append(Text.literal(roleIcon));
        }

        if (!roleName.isEmpty()) {
            result.append(Text.literal(roleName));
        }

        return result;
    }
}
