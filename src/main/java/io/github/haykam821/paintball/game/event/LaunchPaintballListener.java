package io.github.haykam821.paintball.game.event;

import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.game.event.EventType;

public interface LaunchPaintballListener {
	public EventType<LaunchPaintballListener> EVENT = EventType.create(LaunchPaintballListener.class, listeners -> {
		return (world, player) -> {
			for (LaunchPaintballListener listener : listeners) {
				ProjectileEntity projectile = listener.onLaunchProjectile(world, player);
				if (projectile != null) {
					return projectile;
				}
			}
			return null;
		};
	});

	public ProjectileEntity onLaunchProjectile(World world, ServerPlayerEntity player);
}