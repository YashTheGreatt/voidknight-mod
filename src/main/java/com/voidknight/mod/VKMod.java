package com.voidknight.mod;

import net.fabricmc.api.ClientModInitializer;

public class VKMod implements ClientModInitializer {

    public static final String MOD_ID = "voidknight";

    @Override
    public void onInitializeClient() {
        System.out.println("[VoidKnight] Client initialized!");
    }
}
