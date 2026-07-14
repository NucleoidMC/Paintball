package io.github.haykam821.paintball.game.map;

import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;

public class PaintballMap {
	private static final String WAITING_SPAWN_MARKER = "waiting_spawn";
	private static final String SPECTATOR_SPAWN_MARKER = "spectator_spawn";
	private static final String FACING_KEY = "Facing";

	private final MapTemplate template;

	public PaintballMap(MapTemplate template) {
		this.template = template;
	}

	public boolean isOutOfBounds(ServerPlayer player) {
		return !this.template.getBounds().contains(player.blockPosition());
	}

	public boolean teleportToWaitingSpawn(ServerPlayer player) {
		return this.teleportToSpawn(player, WAITING_SPAWN_MARKER);
	}

	public JoinAcceptorResult.Teleport acceptWaitingSpawnOffer(JoinAcceptor acceptor, ServerLevel level) {
		return this.acceptJoins(acceptor, level, WAITING_SPAWN_MARKER);
	}

	public boolean teleportToSpectatorSpawn(ServerPlayer player) {
		return this.teleportToSpawn(player, SPECTATOR_SPAWN_MARKER);
	}

	public JoinAcceptorResult.Teleport acceptSpectatorJoins(JoinAcceptor acceptor, ServerLevel level) {
		return this.acceptJoins(acceptor, level, SPECTATOR_SPAWN_MARKER);
	}

	private JoinAcceptorResult.Teleport acceptJoins(JoinAcceptor acceptor, ServerLevel level, String marker) {
		TemplateRegion region = this.template.getMetadata().getFirstRegion(marker);
		if (region == null) {
			return acceptor.teleport(level, Vec3.ZERO);
		}

		return this.acceptJoins(acceptor, level, region);
	}

	private JoinAcceptorResult.Teleport acceptJoins(JoinAcceptor acceptor, ServerLevel level, TemplateRegion region) {
		Vec3 pos = region.getBounds().centerBottom();
		float facing = region.getData().getFloatOr(FACING_KEY, 0);

		return acceptor.teleport(level, pos).thenRunForEach(player -> {
			player.setYRot(facing);
		});
	}

	public boolean teleportToSpawn(ServerPlayer player, String marker) {
		TemplateRegion region = this.template.getMetadata().getFirstRegion(marker);
		if (region == null) return false;

		this.teleportToSpawn(player, region);
		return true;
	}

	private void teleportToSpawn(ServerPlayer player, TemplateRegion region) {
		Vec3 pos = region.getBounds().centerBottom();
		float facing = region.getData().getFloatOr(FACING_KEY, 0);

		player.teleportTo(player.level(), pos.x(), pos.y(), pos.z(), Set.of(), facing, 0, true);
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}