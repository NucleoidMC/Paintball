package io.github.haykam821.paintball.game.event;

import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface LaunchPaintballEvent {
	public StimulusEvent<LaunchPaintballEvent> EVENT = StimulusEvent.create(LaunchPaintballEvent.class, context -> {
		return (world, player) -> {
			try {
				for (LaunchPaintballEvent listener : context.getListeners()) {
					ProjectileEntity projectile = listener.onLaunchProjectile(world, player);
					if (projectile != null) {
						return projectile;
					}
				}
			} catch (Throwable throwable) {
				context.handleException(throwable);
			}
			return null;
		};
	});

	public ProjectileEntity onLaunchProjectile(World world, ServerPlayerEntity player);
}