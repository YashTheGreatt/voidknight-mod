package com.voidknight.mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VKMod implements ClientModInitializer {
    public static final String MOD_ID = "voidknight";

    @Override
    public void onInitializeClient() {
        try {
            ModContainer container = FabricLoader.getInstance().getModContainer(MOD_ID).orElse(null);
            if (container != null) {
                ResourceManagerHelper.registerBuiltinResourcePack(
                    Identifier.of(MOD_ID, "voidknight_resources"),
                    container,
                    Text.literal("VoidKnight Clan Resources"),
                    ResourcePackActivationType.ALWAYS_ENABLED
                );
            }
        } catch (Throwable ignored) {
        }
    }
}
