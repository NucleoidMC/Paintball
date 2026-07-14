package io.github.haykam821.paintball.game.event;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface LaunchPaintballEvent {
	public StimulusEvent<LaunchPaintballEvent> EVENT = StimulusEvent.create(LaunchPaintballEvent.class, context -> {
		return (level, player) -> {
			try {
				for (LaunchPaintballEvent listener : context.getListeners()) {
					Projectile projectile = listener.onLaunchProjectile(level, player);
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

	public Projectile onLaunchProjectile(Level level, ServerPlayer player);
}