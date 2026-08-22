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
        // Only change player nametags
        if (!(entity instanceof Player player)) {
            return;
        }

        String playerName = player.getName().getString();

        // Get member information
        MemberInfo member = TierManager.getMember(playerName);

        if (member == null) {
            return;
        }

        String role = member.getRole();

        if (role == null || role.trim().isEmpty()) {
            return;
        }

        // Create final nametag
        MutableComponent nameTag = Component.empty();

        // Main VoidKnight icon: E001
        nameTag.append(Component.literal("\uE001"));

        // Separator
        nameTag.append(
                Component.literal(" | ")
                        .withStyle(ChatFormatting.GRAY)
        );

        // Player name with gradient
        nameTag.append(createGradient(playerName));

        // Separator
        nameTag.append(
                Component.literal(" | ")
                        .withStyle(ChatFormatting.GRAY)
        );

        // Get role icon
        String roleIcon = getRoleIcon(member.type);

        // Add role icon if available
        if (!roleIcon.isEmpty()) {
            nameTag.append(Component.literal(roleIcon));
            nameTag.append(Component.literal(" "));
        }

        // Role name with gradient
        nameTag.append(createGradient(role));

        // Set the new nametag
        cir.setReturnValue(nameTag);
    }

    /**
     * Returns the correct custom font character
     * based on the member type.
     *
     * E002 = Sword
     * E003 = Crystal
     * E005 = Grinder
     */
    private String getRoleIcon(String type) {

        if (type == null || type.trim().isEmpty()) {
            return "";
        }

        // Normalize the type so different formats work:
        // "Crystal", "CRYSTAL", "crystal pvp",
        // "crystal_pvp", etc.
        String normalized = type
                .trim()
                .toLowerCase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "");

        // Crystal icon
        if (normalized.contains("crystal")
                || normalized.equals("cpvp")
                || normalized.equals("crystalpvp")) {

            return "\uE003";
        }

        // Grinder icon
        if (normalized.contains("grind")
                || normalized.contains("grinder")
                || normalized.contains("grinding")) {

            return "\uE005";
        }

        // Sword / Combat icon
        if (normalized.contains("combat")
                || normalized.contains("sword")
                || normalized.contains("pvp")
                || normalized.contains("cpvper")) {

            return "\uE002";
        }

        // No matching icon
        return "";
    }

    /**
     * Creates a purple to pink gradient.
     */
    private MutableComponent createGradient(String text) {

        MutableComponent result = Component.empty();

        if (text == null || text.isEmpty()) {
            return result;
        }

        // Start color: Purple
        int startRed = 209;
        int startGreen = 0;
        int startBlue = 255;

        // End color: Pink
        int endRed = 255;
        int endGreen = 77;
        int endBlue = 255;

        int length = Math.max(text.length() - 1, 1);

        for (int i = 0; i < text.length(); i++) {

            float progress = (float) i / length;

            int red = (int) (
                    startRed + (endRed - startRed) * progress
            );

            int green = (int) (
                    startGreen + (endGreen - startGreen) * progress
            );

            int blue = (int) (
                    startBlue + (endBlue - startBlue) * progress
            );

            int color = (red << 16) | (green << 8) | blue;

            result.append(
                    Component.literal(
                                    String.valueOf(text.charAt(i))
                            )
                            .withStyle(style ->
                                    style.withColor(color)
                                            .withBold(true)
                            )
            );
        }

        return result;
    }
}
