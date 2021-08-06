package io.github.haykam821.paintball.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.map.template.TemplateRegion;

public class PaintballMap {
	private static final String WAITING_SPAWN_MARKER = "waiting_spawn";
	private static final String SPECTATOR_SPAWN_MARKER = "spectator_spawn";
	private static final String FACING_KEY = "Facing";

	private final MapTemplate template;

	public PaintballMap(MapTemplate template) {
		this.template = template;
	}

	public boolean isOutOfBounds(ServerPlayerEntity player) {
		return !this.template.getBounds().contains(player.getBlockPos());
	}

	public boolean teleportToWaitingSpawn(ServerPlayerEntity player) {
		return this.teleportToSpawn(player, WAITING_SPAWN_MARKER);
	}

	public boolean teleportToSpectatorSpawn(ServerPlayerEntity player) {
		return this.teleportToSpawn(player, SPECTATOR_SPAWN_MARKER);
	}

	public boolean teleportToSpawn(ServerPlayerEntity player, String marker) {
		TemplateRegion region = this.template.getMetadata().getFirstRegion(marker);
		if (region == null) return false;

		this.teleportToSpawn(player, region);
		return true;
	}

	private void teleportToSpawn(ServerPlayerEntity player, TemplateRegion region) {
		Vec3d pos = region.getBounds().getCenterBottom();
		float facing = region.getData().getFloat(FACING_KEY);

		player.teleport(player.getServerWorld(), pos.getX(), pos.getY(), pos.getZ(), facing, 0);
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}