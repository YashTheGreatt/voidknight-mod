package com.voidknight.mod.mixin;

import com.voidknight.mod.MemberInfo;
import com.voidknight.mod.TierManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

        String playerName = player.getName().getString();
        MemberInfo member = TierManager.getMember(playerName);

        if (member == null) {
            return;
        }

        String role = member.getRole();

        if (role == null || role.isBlank()) {
            return;
        }

        String roleIcon = getRoleIcon(member.type, role);

        MutableComponent nameTag = Component.empty();

        // VK ICON
        nameTag.append(Component.literal("\uE001"));

        // |
        nameTag.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));

        // USERNAME
        nameTag.append(createGradient(playerName));

        // |
        nameTag.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));

        // ROLE ICON
        if (!roleIcon.isEmpty()) {
            nameTag.append(Component.literal(roleIcon));
            nameTag.append(Component.literal(" "));
        }

        // ROLE
        nameTag.append(createGradient(role));

        cir.setReturnValue(nameTag);
    }

    private String getRoleIcon(String type, String role) {
        String value = "";

        if (type != null && !type.isBlank()) {
            value = type.toLowerCase();
        } else if (role != null) {
            value = role.toLowerCase();
        }

        // TYPE BASED ICONS
        switch (value) {
            case "combat":
                return "\uE002";

            case "crystal":
                return "\uE003";

            case "builder":
                return "\uE004";

            case "grinder":
                return "\uE005";
        }

        // ROLE NAME FALLBACK
        if (role != null) {
            String roleLower = role.toLowerCase();

            if (roleLower.contains("pvp")) {
                return "\uE002";
            }

            if (roleLower.contains("crystal")) {
                return "\uE003";
            }

            if (roleLower.contains("builder")) {
                return "\uE004";
            }

            if (roleLower.contains("grinder")) {
                return "\uE005";
            }
        }

        return "";
    }

    private MutableComponent createGradient(String text) {
        MutableComponent result = Component.empty();

        int startRed = 209;
        int startGreen = 0;
        int startBlue = 255;

        int endRed = 255;
        int endGreen = 77;
        int endBlue = 255;

        int length = Math.max(text.length() - 1, 1);

        for (int i = 0; i < text.length(); i++) {
            float progress = (float) i / length;

            int red = (int) (startRed + (endRed - startRed) * progress);
            int green = (int) (startGreen + (endGreen - startGreen) * progress);
            int blue = (int) (startBlue + (endBlue - startBlue) * progress);

            int color = (red << 16) | (green << 8) | blue;

            result.append(
                    Component.literal(String.valueOf(text.charAt(i)))
                            .withStyle(style ->
                                    style.withColor(color).withBold(true)
                            )
            );
        }

        return result;
    }
}
