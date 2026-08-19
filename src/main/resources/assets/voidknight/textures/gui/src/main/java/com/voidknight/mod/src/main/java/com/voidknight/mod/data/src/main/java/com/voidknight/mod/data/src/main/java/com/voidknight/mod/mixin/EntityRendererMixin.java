package com.voidknight.mod.mixin;

import com.voidknight.mod.data.DataManager;
import com.voidknight.mod.data.MemberData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @ModifyVariable(
        method = "renderLabelIfPresent",
        at = @At("HEAD"),
        argsOnly = true
    )
    protected Text modifyNametag(Text originalText, T entity, MatrixStack matrices, 
                                 VertexConsumerProvider vertexConsumers, int light) {
        if (entity instanceof AbstractClientPlayerEntity player) {
            String ign = player.getGameProfile().getName();

            if (DataManager.MEMBERS.containsKey(ign)) {
                MemberData data = DataManager.MEMBERS.get(ign);
                String tag = "§8[§4VK§8] §7| §f" + ign;

                if ("CPvP".equalsIgnoreCase(data.mode)) {
                    return Text.literal(tag + " §7| §d✦ §e" + data.tier);
                } else if ("Sword".equalsIgnoreCase(data.mode)) {
                    return Text.literal(tag + " §7| §c⚔ §e" + data.tier);
                } else if ("Grinder".equalsIgnoreCase(data.mode)) {
                    return Text.literal(tag + " §7| §a⛏ Grinder");
                } else if ("Builder".equalsIgnoreCase(data.mode)) {
                    return Text.literal(tag + " §7| §6🏗 Builder");
                }
            }
        }
        return originalText;
    }
}
