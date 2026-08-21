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

```
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

    if (role.isEmpty()) {
        return;
    }

    MutableComponent nameTag = Component.empty();

    // VK icon
    nameTag.append(Component.literal("\uE001"));

    // |
    nameTag.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));

    // Username
    nameTag.append(createGradient(playerName));

    // |
    nameTag.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));

    // Role icon
    nameTag.append(Component.literal(getRoleIcon(member.type)));

    nameTag.append(Component.literal(" "));

    // Role — API ki original capitalization preserve hogi
    nameTag.append(createGradient(role));

    cir.setReturnValue(nameTag);
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
```

}
