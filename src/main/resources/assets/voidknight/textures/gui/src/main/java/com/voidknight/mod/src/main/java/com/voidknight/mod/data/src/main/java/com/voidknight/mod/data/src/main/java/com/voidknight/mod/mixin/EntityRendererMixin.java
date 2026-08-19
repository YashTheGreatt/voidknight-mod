package com.voidknight.mod.mixin;

import com.voidknight.mod.DataManager;
import com.voidknight.mod.MemberData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

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

        String displayName = "§5[VK] §f" + playerName;
        float x = -textRenderer.getWidth(displayName) / 2.0F;

        textRenderer.draw(
            displayName,
            x,
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
