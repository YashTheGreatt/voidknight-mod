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

    private static final Identifier VK_ICON = Identifier.of("voidknight", "icon.png");

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

        // Custom tags: PvP Category, Tier, and Clan Role (Builder/Grinder)
        String pvpType = member.getPvpType() != null ? " §c[" + member.getPvpType() + "]" : " §c[CRYSTAL]";
        String tierText = member.getTier() != null ? " §a[" + member.getTier() + "]" : "";
        String roleText = member.getRole() != null ? " §6[" + member.getRole() + "]" : "";

        String fullTag = "§f" + playerName + pvpType + tierText + roleText;

        float tagWidth = textRenderer.getWidth(fullTag);
        float iconSize = 9.0F;
        float spacing = 3.0F;
        float totalWidth = iconSize + spacing + tagWidth;
        float startX = -totalWidth / 2.0F;

        // 1. VK Logo Render
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, VK_ICON);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float ix1 = startX;
        float ix2 = startX + iconSize;
        float iy1 = -1.0F;
        float iy2 = iy1 + iconSize;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix4f, ix1, iy2, 0.0F).texture(0.0F, 1.0F);
        buffer.vertex(matrix4f, ix2, iy2, 0.0F).texture(1.0F, 1.0F);
        buffer.vertex(matrix4f, ix2, iy1, 0.0F).texture(1.0F, 0.0F);
        buffer.vertex(matrix4f, ix1, iy1, 0.0F).texture(0.0F, 0.0F);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();

        // 2. Nametag Render
        float textX = startX + iconSize + spacing;
        textRenderer.draw(
            fullTag,
            textX,
            0,
            0xFFFFFF,
            false,
            matrix4f,
            vertexConsumers,
            TextRenderer.TextLayerType.SEE_THROUGH,
            0x40000000,
            light
        );

        matrices.pop();
    }
}
