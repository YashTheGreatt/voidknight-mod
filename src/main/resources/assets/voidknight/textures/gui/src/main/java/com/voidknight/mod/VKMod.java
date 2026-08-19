package com.voidknight.mod;

import com.voidknight.mod.data.DataManager;
import net.fabricmc.api.ClientModInitializer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VKMod implements ClientModInitializer {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onInitializeClient() {
        scheduler.scheduleAtFixedRate(DataManager::fetchClanData, 0, 60, TimeUnit.SECONDS);
    }
}
