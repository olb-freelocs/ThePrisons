package com.freelocs.theprisons.modmenu;

import com.freelocs.theprisons.gui.ThePrisonsConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class ThePrisonsModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ThePrisonsConfigScreen::new;
    }
}
