package com.voidknight.mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VKMod implements ModInitializer {
    public static final String MOD_ID = "voidknight";

    @Override
    public void onInitialize() {
        ModContainer container = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        
        ResourceManagerHelper.registerBuiltinResourcePack(
            Identifier.of(MOD_ID, "voidknight_resources"),
            container,
            Text.literal("VoidKnight Clan Resources"),
            ResourcePackActivationType.ALWAYS_ENABLED
        );
    }
}
