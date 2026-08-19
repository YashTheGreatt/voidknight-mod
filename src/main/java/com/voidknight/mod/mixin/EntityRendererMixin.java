package com.voidknight.mod.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    private static final ConcurrentHashMap<String, MemberInfo> MEMBERS = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    private static synchronized void initSync() {
        if (initialized) return;
        initialized = true;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VoidKnight-Sync");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(EntityRendererMixin::fetchData, 0, 10, TimeUnit.SECONDS);
    }

    private static void fetchData() {
        try {
            URL url = new URL("https://voidknight.onrender.com/api/tiers");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "VoidKnightMod/1.0");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                    JsonElement root = JsonParser.parseReader(reader);
                    Gson gson = new Gson();
                    ConcurrentHashMap<String, MemberInfo> temp = new ConcurrentHashMap<>();

                    if (root.isJsonObject()) {
                        JsonObject obj = root.getAsJsonObject();
                        for (String key : obj.keySet()) {
                            MemberInfo m = gson.fromJson(obj.get(key), MemberInfo.class);
                            if (m != null) {
                                temp.put(key.toLowerCase().trim(), m);
                            }
                        }
                    }
                    if (!temp.isEmpty()) {
                        MEMBERS.clear();
                        MEMBERS.putAll(temp);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @ModifyVariable(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text modifyNametagText121(Text originalText, AbstractClientPlayerEntity player) {
        return buildCustomText(originalText, player);
    }

    @ModifyVariable(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text modifyNametagTextFallback(Text originalText, AbstractClientPlayerEntity player) {
        return buildCustomText(originalText, player);
    }

    private Text buildCustomText(Text originalText, AbstractClientPlayerEntity player) {
        if (!initialized) {
            initSync();
        }

        if (player == null || MEMBERS.isEmpty()) return originalText;

        String cleanIGN = player.getGameProfile().getName();
        if (cleanIGN == null || cleanIGN.isEmpty()) return originalText;

        MemberInfo member = MEMBERS.get(cleanIGN.toLowerCase().trim());
        if (member == null) return originalText;

        String vkIconChar = "\uE001 ";
        String modeIconChar = "";

        String modeVal = member.mode != null ? member.mode : member.role;
        if (modeVal != null) {
            String m = modeVal.toLowerCase();
            if (m.contains("cpvp") || m.contains("crystal")) modeIconChar = " \uE002";
            else if (m.contains("spvp") || m.contains("sword")) modeIconChar = " \uE003";
        }

        String tierText = (member.tier != null && !member.tier.isEmpty()) ? " §e[" + member.tier + "]" : "";

        MutableText newText = Text.literal(vkIconChar);
        newText.append(originalText);
        newText.append(Text.literal(tierText + modeIconChar));
        return newText;
    }

    private static class MemberInfo {
        String mode;
        String tier;
        String type;
        String role;
    }
}
