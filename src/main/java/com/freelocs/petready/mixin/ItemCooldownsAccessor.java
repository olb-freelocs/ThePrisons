package com.freelocs.petready.mixin;

import net.minecraft.entity.player.ItemCooldownManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ItemCooldownManager.class)
public interface ItemCooldownsAccessor {
    @Accessor("entries")
    Map<?, ?> petready$getCooldowns();

    @Accessor("tick")
    int petready$getTickCount();
}
