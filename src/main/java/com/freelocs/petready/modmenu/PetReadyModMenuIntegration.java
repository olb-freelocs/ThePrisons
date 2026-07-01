package com.freelocs.petready.modmenu;

import com.freelocs.petready.gui.PetReadyConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class PetReadyModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PetReadyConfigScreen::new;
    }
}
