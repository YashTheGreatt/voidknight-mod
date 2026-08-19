package com.voidknight.mod;

import net.fabricmc.api.ClientModInitializer;

public class VKMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Auto-sync start
        DataManager.startAutoSync();
    }
}
