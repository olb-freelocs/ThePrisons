package com.freelocs.petready;

import com.freelocs.petready.cache.PetCooldownCache;
import com.freelocs.petready.config.ConfigManager;
import com.freelocs.petready.state.PetCooldownTracker;
import com.freelocs.petready.ui.PetReadyHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PetReadyClient implements ClientModInitializer {
    public static final String MOD_ID = "petready";
    public static final Logger LOGGER = LoggerFactory.getLogger("PetReady");

    public static final ConfigManager CONFIG = new ConfigManager();
    public static final PetCooldownCache CACHE = new PetCooldownCache();
    public static final PetCooldownTracker TRACKER = new PetCooldownTracker();

    @Override
    public void onInitializeClient() {
        CONFIG.load();
        CACHE.load();

        ClientTickEvents.END_CLIENT_TICK.register(TRACKER::tick);
        HudRenderCallback.EVENT.register(PetReadyHudRenderer::render);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CACHE.save();
            CONFIG.save();
        });

        LOGGER.info("PetReady initialized");
    }
}
