package com.voidknight.mod.mixin;

import com.voidknight.mod.DataManager;
import com.voidknight.mod.MemberData;
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

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    private static final Identifier VK_ICON = Identifier.of("voidknight", "textures/gui/vk.png");
    private static final Identifier CRYSTAL_ICON = Identifier.of("voidknight", "textures/gui/crystal.png");
    private static final Identifier SWORD_ICON = Identifier.of("voidknight", "textures/gui/sword.png");
    private static final Identifier BUILDER_ICON = Identifier.of("voidknight", "textures/gui/builder.png");
    private static final Identifier GRINDER_ICON = Identifier.of("voidknight", "textures/gui/grinder.png");

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player)) return;

        String playerName = player.getNameForScoreboard();
        MemberData member = DataManager.getMember(playerName);
        if (member == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        if (textRenderer == null) return;

        ci.cancel();

        double distanceSq = client.getEntityRenderDispatcher().getSquaredDistanceToCamera(entity);
        if (distanceSq > 4096.0) return;

        matrices.push();
        matrices.translate(0.0F, entity.getNameLabelHeight() + 0.5F, 0.0F);
        matrices.multiply(client.getEntityRenderDispatcher().getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        // 1. Text Setup: [IGN] [Tier]
        String tierText = (member.getTier() != null && !member.getTier().isEmpty()) ? " §e[" + member.getTier() + "]" : "";
        String fullText = "§f" + playerName + tierText;

        float textWidth = textRenderer.getWidth(fullText);
        float iconSize = 9.0F;
        float spacing = 2.5F;

        // 2. Logic: PvP Icon (Based on pvpType) vs Role Icon (Builder/Grinder)
        Identifier pvpIcon = null;
        if (member.getPvpType() != null) {
            if (member.getPvpType().toLowerCase().contains("sword")) pvpIcon = SWORD_ICON;
            else if (member.getPvpType().toLowerCase().contains("crystal")) pvpIcon = CRYSTAL_ICON;
        }

        Identifier roleIcon = null;
        if (member.getRole() != null) {
            if (member.getRole().toLowerCase().contains("builder")) roleIcon = BUILDER_ICON;
            else if (member.getRole().toLowerCase().contains("grinder")) roleIcon = GRINDER_ICON;
        }

        // 3. Draw Sequence
        float totalWidth = iconSize + spacing + textWidth;
        if (pvpIcon != null) totalWidth += spacing + iconSize;
        if (roleIcon != null) totalWidth += spacing + iconSize;

        float currentX = -totalWidth / 2.0F;

        // Draw VK Icon
        drawIcon(matrix4f, VK_ICON, currentX, -1.0F, iconSize);
        currentX += iconSize + spacing;

        // Draw Name + Tier
        textRenderer.draw(Text.literal(fullText), currentX, 0, 0xFFFFFF, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0x40000000, light);
        currentX += textWidth + spacing;

        // Draw PvP Icon
        if (pvpIcon != null) {
            drawIcon(matrix4f, pvpIcon, currentX, -1.0F, iconSize);
            currentX += iconSize + spacing;
        }

        // Draw Role Icon
        if (roleIcon != null) {
            drawIcon(matrix4f, roleIcon, currentX, -1.0F, iconSize);
        }

        matrices.pop();
    }

    private void drawIcon(Matrix4f matrix4f, Identifier icon, float x, float y, float size) {
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
    }
}
