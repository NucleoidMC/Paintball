package io.github.haykam821.paintball.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

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

	public PlayerOfferResult.Accept acceptWaitingSpawnOffer(PlayerOffer offer, ServerWorld world) {
		return this.acceptOffer(offer, world, WAITING_SPAWN_MARKER);
	}

	public boolean teleportToSpectatorSpawn(ServerPlayerEntity player) {
		return this.teleportToSpawn(player, SPECTATOR_SPAWN_MARKER);
	}

	public PlayerOfferResult.Accept acceptSpectatorSpawnOffer(PlayerOffer offer, ServerWorld world) {
		return this.acceptOffer(offer, world, SPECTATOR_SPAWN_MARKER);
	}

	private PlayerOfferResult.Accept acceptOffer(PlayerOffer offer, ServerWorld world, String marker) {
		TemplateRegion region = this.template.getMetadata().getFirstRegion(marker);
		if (region == null) {
			return offer.accept(world, Vec3d.ZERO);
		}

		return this.acceptOffer(offer, world, region);
	}

	private PlayerOfferResult.Accept acceptOffer(PlayerOffer offer, ServerWorld world, TemplateRegion region) {
		Vec3d pos = region.getBounds().centerBottom();
		float facing = region.getData().getFloat(FACING_KEY);

		return offer.accept(world, pos).and(() -> {
			offer.player().setYaw(facing);
		});
	}

	public boolean teleportToSpawn(ServerPlayerEntity player, String marker) {
		TemplateRegion region = this.template.getMetadata().getFirstRegion(marker);
		if (region == null) return false;

		this.teleportToSpawn(player, region);
		return true;
	}

	private void teleportToSpawn(ServerPlayerEntity player, TemplateRegion region) {
		Vec3d pos = region.getBounds().centerBottom();
		float facing = region.getData().getFloat(FACING_KEY);

		player.teleport(player.getServerWorld(), pos.getX(), pos.getY(), pos.getZ(), facing, 0);
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}