package com.freelocs.theprisons;

import com.freelocs.theprisons.cache.ThePrisonsCache;
import com.freelocs.theprisons.config.ThePrisonsConfigManager;
import com.freelocs.theprisons.state.ThePrisonsTracker;
import com.freelocs.theprisons.ui.ThePrisonsHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThePrisonsClient implements ClientModInitializer {
    public static final String MOD_ID = "theprisons";
    public static final Logger LOGGER = LoggerFactory.getLogger("ThePrisons");

    public static final ThePrisonsConfigManager CONFIG = new ThePrisonsConfigManager();
    public static final ThePrisonsCache CACHE = new ThePrisonsCache();
    public static final ThePrisonsTracker TRACKER = new ThePrisonsTracker();

    @Override
    public void onInitializeClient() {
        CONFIG.load();
        CACHE.load();

        ClientTickEvents.END_CLIENT_TICK.register(TRACKER::tick);
        HudRenderCallback.EVENT.register(ThePrisonsHudRenderer::render);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CACHE.save();
            CONFIG.save();
        });

        LOGGER.info("ThePrisons initialized");
    }
}
