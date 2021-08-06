package io.github.haykam821.paintball.game;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.player.GameTeam;

public class PaintballConfig {
	public static final Codec<PaintballConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("map").forGetter(PaintballConfig::getMap),
			PlayerConfig.CODEC.fieldOf("players").forGetter(PaintballConfig::getPlayerConfig),
			GameTeam.CODEC.listOf().fieldOf("teams").forGetter(PaintballConfig::getTeams),
			Codec.INT.optionalFieldOf("max_damage", 4).forGetter(PaintballConfig::getMaxDamage),
			Codec.BOOL.optionalFieldOf("allow_friendly_fire", false).forGetter(PaintballConfig::shouldAllowFriendlyFire)
		).apply(instance, PaintballConfig::new);
	});

	private final Identifier map;
	private final PlayerConfig playerConfig;
	private final List<GameTeam> teams;
	private final int maxDamage;
	private final boolean allowFriendlyFire;

	public PaintballConfig(Identifier map, PlayerConfig playerConfig, List<GameTeam> teams, int maxDamage, boolean allowFriendlyFire) {
		this.map = map;
		this.playerConfig = playerConfig;
		this.teams = teams;
		this.maxDamage = maxDamage;
		this.allowFriendlyFire = allowFriendlyFire;
	}

	public Identifier getMap() {
		return this.map;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public List<GameTeam> getTeams() {
		return this.teams;
	}

	public int getMaxDamage() {
		return this.maxDamage;
	}

	public boolean shouldAllowFriendlyFire() {
		return this.allowFriendlyFire;
	}
}