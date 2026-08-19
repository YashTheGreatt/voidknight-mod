package com.voidknight.mod;

import net.fabricmc.api.ClientModInitializer;

public class VKMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("[VoidKnight] Mod loaded successfully!");
    }
}
