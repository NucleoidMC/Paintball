package io.github.haykam821.paintball.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.common.team.GameTeamList;

public class PaintballConfig {
	public static final Codec<PaintballConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("map").forGetter(PaintballConfig::getMap),
			PlayerConfig.CODEC.fieldOf("players").forGetter(PaintballConfig::getPlayerConfig),
			GameTeamList.CODEC.fieldOf("teams").forGetter(PaintballConfig::getTeams),
			Codec.INT.optionalFieldOf("max_damage", 4).forGetter(PaintballConfig::getMaxDamage),
			Codec.BOOL.optionalFieldOf("allow_friendly_fire", false).forGetter(PaintballConfig::shouldAllowFriendlyFire),
			Codec.INT.optionalFieldOf("stain_radius", 2).forGetter(PaintballConfig::getStainRadius)
		).apply(instance, PaintballConfig::new);
	});

	private final Identifier map;
	private final PlayerConfig playerConfig;
	private final GameTeamList teams;
	private final int maxDamage;
	private final boolean allowFriendlyFire;
	private final int stainRadius;

	public PaintballConfig(Identifier map, PlayerConfig playerConfig, GameTeamList teams, int maxDamage, boolean allowFriendlyFire, int stainRadius) {
		this.map = map;
		this.playerConfig = playerConfig;
		this.teams = teams;
		this.maxDamage = maxDamage;
		this.allowFriendlyFire = allowFriendlyFire;
		this.stainRadius = stainRadius;
	}

	public Identifier getMap() {
		return this.map;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public GameTeamList getTeams() {
		return this.teams;
	}

	public int getMaxDamage() {
		return this.maxDamage;
	}

	public boolean shouldAllowFriendlyFire() {
		return this.allowFriendlyFire;
	}

	public int getStainRadius() {
		return this.stainRadius;
	}
}