package com.freelocs.petready.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface ItemCooldownInstanceAccessor {
    @Accessor("startTick")
    int petready$getStartTime();

    @Accessor("endTick")
    int petready$getEndTime();
}
