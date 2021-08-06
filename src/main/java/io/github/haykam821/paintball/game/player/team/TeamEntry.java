package io.github.haykam821.paintball.game.player.team;

import org.apache.commons.lang3.RandomStringUtils;

import io.github.haykam821.paintball.game.phase.PaintballActivePhase;
import io.github.haykam821.paintball.game.player.armor.ArmorSet;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public class TeamEntry {
	private final PaintballActivePhase phase;
	private final GameTeam gameTeam;
	private final Team scoreboardTeam;
	private final ArmorSet armorSet;

	public TeamEntry(PaintballActivePhase phase, GameTeam gameTeam, MinecraftServer server) {
		this.phase = phase;
		this.gameTeam = gameTeam;

		ServerScoreboard scoreboard = server.getScoreboard();
		String key = RandomStringUtils.randomAlphanumeric(16);
		this.scoreboardTeam = TeamEntry.getOrCreateScoreboardTeam(key, scoreboard);
		this.initializeTeam();

		this.armorSet = new ArmorSet(gameTeam.getColor());
	}

	// Getters
	public GameTeam getGameTeam() {
		return this.gameTeam;
	}

	public Team getScoreboardTeam() {
		return this.scoreboardTeam;
	}

	public ArmorSet getArmorSet() {
		return this.armorSet;
	}

	// Utilities
	public Text getName() {
		return new LiteralText(this.gameTeam.getDisplay())
			.formatted(this.gameTeam.getFormatting());
	}

	public Text getUncoloredName() {
		return new LiteralText(this.gameTeam.getDisplay());
	}

	public boolean teleportToSpawn(ServerPlayerEntity player) {
		return this.phase.getMap().teleportToSpawn(player, this.gameTeam.getKey() + "_spawn");
	}

	private void initializeTeam() {
		// Display
		this.scoreboardTeam.setDisplayName(this.getUncoloredName());
		this.scoreboardTeam.setColor(this.gameTeam.getFormatting());

		// Rules
		this.scoreboardTeam.setFriendlyFireAllowed(false);
		this.scoreboardTeam.setShowFriendlyInvisibles(true);
		this.scoreboardTeam.setCollisionRule(Team.CollisionRule.NEVER);
	}

	private static Team getOrCreateScoreboardTeam(String key, ServerScoreboard scoreboard) {
		Team scoreboardTeam = scoreboard.getTeam(key);
		if (scoreboardTeam == null) {
			return scoreboard.addTeam(key);
		}
		return scoreboardTeam;
	}

	@Override
	public String toString() {
		return "TeamEntry{phase=" + this.phase + ", gameTeam=" + this.gameTeam + ", scoreboardTeam=" + this.scoreboardTeam + ", armorSet=" + this.armorSet + "}";
	}
}
