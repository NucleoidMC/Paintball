package io.github.haykam821.paintball.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.decoration.ArmorStand;

@Mixin(ArmorStand.class)
public interface ArmorStandAccessor {
	@Accessor("disabledSlots")
	public void paintball$setDisabledSlots(int disabledSlots);
}
