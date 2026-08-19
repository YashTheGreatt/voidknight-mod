package com.voidknight.mod.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    private static final Identifier VK_ICON = Identifier.of("voidknight", "textures/gui/vk.png");
    private static final Identifier CRYSTAL_ICON = Identifier.of("voidknight", "textures/gui/crystal.png");
    private static final Identifier SWORD_ICON = Identifier.of("voidknight", "textures/gui/sword.png");
    private static final Identifier BUILDER_ICON = Identifier.of("voidknight", "textures/gui/builder.png");
    private static final Identifier GRINDER_ICON = Identifier.of("voidknight", "textures/gui/grinder.png");

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
        scheduler.scheduleAtFixedRate(EntityRendererMixin::fetchData, 0, 15, TimeUnit.SECONDS);
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

                    if (root.isJsonArray()) {
                        JsonArray array = root.getAsJsonArray();
                        for (JsonElement el : array) {
                            MemberInfo m = gson.fromJson(el, MemberInfo.class);
                            if (m != null && m.ign != null && !m.ign.isEmpty()) {
                                temp.put(m.ign.toLowerCase().trim(), m);
                            }
                        }
                    } else if (root.isJsonObject()) {
                        JsonObject obj = root.getAsJsonObject();
                        for (String key : obj.keySet()) {
                            MemberInfo m = gson.fromJson(obj.get(key), MemberInfo.class);
                            if (m != null) {
                                temp.put(key.toLowerCase().trim(), m);
                            }
                        }
                    }
                    MEMBERS.clear();
                    MEMBERS.putAll(temp);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderLabel121(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, CallbackInfo ci) {
        renderCustomNametag(entity, text, matrices, vertexConsumers, light, ci);
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderLabelFallback(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        renderCustomNametag(entity, text, matrices, vertexConsumers, light, ci);
    }

    private void renderCustomNametag(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!initialized) {
            initSync();
        }

        try {
            if (!(entity instanceof PlayerEntity player)) return;

            // Pure IGN fetch karein bina scoreboard rank prefix ke
            String cleanIGN = player.getGameProfile().getName();
            if (cleanIGN == null || cleanIGN.isEmpty() || MEMBERS.isEmpty()) return;

            MemberInfo member = MEMBERS.get(cleanIGN.toLowerCase().trim());
            if (member == null) return;

            MinecraftClient client = MinecraftClient.getInstance();
            TextRenderer textRenderer = client.textRenderer;
            if (textRenderer == null) return;

            ci.cancel();

            double distanceSq = client.getEntityRenderDispatcher().getSquaredDistanceToCamera(entity);
            if (distanceSq > 4096.0) return;

            matrices.push();
            matrices.translate(0.0F, player.getStandingEyeHeight() + 0.5F, 0.0F);
            matrices.multiply(client.getEntityRenderDispatcher().getRotation());
            matrices.scale(-0.025F, -0.025F, 0.025F);

            Matrix4f matrix4f = matrices.peek().getPositionMatrix();

            String tierText = (member.tier != null && !member.tier.isEmpty()) ? " §e[" + member.tier + "]" : "";
            String fullText = "§f" + cleanIGN + tierText;

            float textWidth = textRenderer.getWidth(fullText);
            float iconSize = 9.0F;
            float spacing = 2.5F;

            // Role / PvP Check (Supports both CPvP, SPvP, crystal, sword)
            Identifier pvpIcon = null;
            String roleVal = (member.role != null ? member.role : member.pvpType);
            if (roleVal != null) {
                String r = roleVal.toLowerCase();
                if (r.contains("crystal") || r.contains("cpvp")) pvpIcon = CRYSTAL_ICON;
                else if (r.contains("sword") || r.contains("spvp")) pvpIcon = SWORD_ICON;
                else if (r.contains("builder")) pvpIcon = BUILDER_ICON;
                else if (r.contains("grinder")) pvpIcon = GRINDER_ICON;
            }

            float totalWidth = iconSize + spacing + textWidth;
            if (pvpIcon != null) totalWidth += spacing + iconSize;

            float currentX = -totalWidth / 2.0F;

            // VoidKnight Logo Render
            drawIcon(matrix4f, VK_ICON, currentX, -1.0F, iconSize);
            currentX += iconSize + spacing;

            // Name + Tier Render
            textRenderer.draw(Text.literal(fullText), currentX, 0, 0xFFFFFF, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0x40000000, light);
            currentX += textWidth + spacing;

            // PvP / Tier Icon Render
            if (pvpIcon != null) {
                drawIcon(matrix4f, pvpIcon, currentX, -1.0F, iconSize);
            }

            matrices.pop();
        } catch (Exception ignored) {
        }
    }

    private void drawIcon(Matrix4f matrix4f, Identifier icon, float x, float y, float size) {
        try {
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, icon);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            buffer.vertex(matrix4f, x, y + size, 0.0F).texture(0.0F, 1.0F);
            buffer.vertex(matrix4f, x + size, y + size, 0.0F).texture(1.0F, 1.0F);
            buffer.vertex(matrix4f, x + size, y, 0.0F).texture(1.0F, 0.0F);
            buffer.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.disableBlend();
        } catch (Exception ignored) {
        }
    }

    private static class MemberInfo {
        String ign;
        String pvpType;
        String tier;
        String role;
    }
}
