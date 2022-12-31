package com.ugackminer.discordproximity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.command.EntitySelector;

@Mixin(EntitySelector.class)
public interface EntitySelectorAccessor {
    @Accessor
    String getPlayerName();
}
