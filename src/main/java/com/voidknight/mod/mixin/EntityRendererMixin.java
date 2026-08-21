package com.voidknight.mod.mixin;

import com.voidknight.mod.MemberInfo;
import com.voidknight.mod.TierManager;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(
            method = "getNameTag",
            at = @At("RETURN"),
            cancellable = true
    )
    private void voidknight$changeNameTag(
            Entity entity,
            CallbackInfoReturnable<Component> cir
    ) {
        if (!(entity instanceof Player player)) {
            return;
        }

        String playerName = player.getGameProfile().getName();
        MemberInfo member = TierManager.getMember(playerName);

        // Player API mein nahi hai = normal nametag
        if (member == null) {
            return;
        }

        String role = member.getRole();

        if (role.isEmpty()) {
            return;
        }

        String roleIcon = getRoleIcon(member.type);

        cir.setReturnValue(Component.literal(
                "\uE001 " +
                playerName +
                " " +
                roleIcon +
                " [" + role + "]"
        ));
    }

    private String getRoleIcon(String type) {
        if (type == null) {
            return "";
        }

        return switch (type.toLowerCase()) {
            case "combat" -> "\uE002";
            case "crystal" -> "\uE003";
            case "builder" -> "\uE004";
            case "grinder" -> "\uE005";
            default -> "";
        };
    }
}
