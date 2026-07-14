package io.github.haykam821.paintball.game.player;

import java.util.Set;

import io.github.haykam821.paintball.game.StainRemovalConfig;
import io.github.haykam821.paintball.game.item.PaintballItems;
import io.github.haykam821.paintball.game.phase.PaintballActivePhase;
import io.github.haykam821.paintball.game.player.armor.ArmorSet;
import io.github.haykam821.paintball.game.player.team.TeamEntry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;
import xyz.nucleoid.plasmid.api.util.PlayerUtil;

public class PlayerEntry {
	private static final ItemStack HAND_STACK = ItemStackBuilder.of(PaintballItems.PAINTBALL_LAUNCHER)
		.setUnbreakable()
		.build();

	private static final int RAIN_PARTICLE_COUNT = 50;

	private final PaintballActivePhase phase;
	private final ServerPlayer player;
	private final TeamEntry team;

	private int damage = 0;
	private PlayerEntry lastDamager;

	private RevivalMarker marker;
	private int stainRemovers;

	public PlayerEntry(PaintballActivePhase phase, ServerPlayer player, TeamEntry team) {
		this.phase = phase;
		this.player = player;
		this.team = team;

		StainRemovalConfig stainRemoval = this.phase.getConfig().getStainRemoval();
		RandomSource random = this.player.getRandom();

		this.stainRemovers = stainRemoval.initialCount().sample(random);
	}

	// Getters
	public ServerPlayer getPlayer() {
		return this.player;
	}

	public TeamEntry getTeam() {
		return this.team;
	}

	// Utilities
	public void spawn(boolean spectator) {
		// Position
		if (spectator) {
			this.phase.getMap().teleportToSpectatorSpawn(this.player);
		} else {
			this.team.teleportToSpawn(this.player);
		}

		this.spawnWithoutTeleport(spectator);
	}

	public void spawnAtRevivalMarker(RevivalMarker marker, boolean spectator) {
		Vec3 pos = marker.getPos();
		double y = pos.y() + (spectator ? 1 : 0);

		player.teleportTo(player.level(), pos.x(), y, pos.z(), Set.of(), marker.getYaw(), marker.getPitch(), true);

		this.spawnWithoutTeleport(spectator);

		if (!spectator) {
			marker.destroy();
		}
	}

	private void spawnWithoutTeleport(boolean spectator) {
		// State
		this.player.setGameMode(spectator ? GameType.SPECTATOR : GameType.SURVIVAL);
		this.player.setAirSupply(this.player.getMaxAirSupply());
		this.player.setRemainingFireTicks(0);
		this.player.fallDistance = 0;
		this.player.removeAllEffects();

		// Inventory
		this.player.getInventory().clearContent();
		this.player.setExperienceLevels(0);
		this.player.setExperiencePoints(0);

		if (!spectator) {
			this.player.addItem(HAND_STACK.copy());
			this.phase.getConfig().getStainRemoval().give(this.player, this.stainRemovers);
			this.applyDamageRepresentation(0);
		}
	}

	protected float getDamageProgress() {
		int maxDamage = this.phase.getConfig().getMaxDamage();
		if (maxDamage == 0) {
			return 1;
		}

		return this.damage / (float) maxDamage;
	}

	private ArmorSet getArmorSetForDamageProgress(float damageProgress, float minStainedDamage) {
		return damageProgress >= minStainedDamage ? this.phase.getStainedArmorSet() : this.team.getArmorSet();
	}

	protected void applyArmor(float damageProgress, LivingEntity entity) {
		entity.setItemSlot(EquipmentSlot.HEAD, (this.getArmorSetForDamageProgress(damageProgress, 1)).getHelmet(this.team));
		entity.setItemSlot(EquipmentSlot.CHEST, (this.getArmorSetForDamageProgress(damageProgress, 0.5f)).getChestplate(this.team));
		entity.setItemSlot(EquipmentSlot.LEGS, (this.getArmorSetForDamageProgress(damageProgress, 0.75f)).getLeggings(this.team));
		entity.setItemSlot(EquipmentSlot.FEET, (this.getArmorSetForDamageProgress(damageProgress, 0.25f)).getBoots(this.team));
	}

	private void applyHealth(float damageProgress) {
		float maxHealth = this.player.getMaxHealth();

		float health = (maxHealth + 1) - (damageProgress * maxHealth);
		if (health < 1) {
			health = 1;
		}

		this.player.setHealth(health);
	}

	/**
	 * Applies damage representation in the form of health and armor.
	 */
	private void applyDamageRepresentation(float damageProgress) {
		this.applyArmor(damageProgress, this.player);
		this.applyHealth(damageProgress);

		this.player.containerMenu.broadcastChanges();
		this.player.inventoryMenu.slotsChanged(this.player.getInventory());
	}

	/**
	 * Damages the player by one unit and applies their damage representation accordingly.
	 */
	public void damage(PlayerEntry damager) {
		this.damage += 1;

		if (damager != null) {
			this.lastDamager = damager;
		}

		float pitch = (this.player.getRandom().nextFloat() * 0.3f) + 1.2f;
		PlayerUtil.playSoundToPlayer(player, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1, pitch);

		this.applyDamageRepresentation(this.getDamageProgress());
	}

	/**
	 * Revives or removes damage from the player based on stain removal configuration and updates player state accordingly.
	 */
	public void recover(StainRemovalConfig config) {
		int oldDamage = this.damage;
		this.damage = config.modifyDamage(this.player, this.damage);

		if (!this.isAlive() && this.getDamageProgress() <= 1) {
			this.spawnAtRevivalMarker(this.marker, false);
			this.marker = null;
		}

		// Apply heart particles
		RandomSource random = this.player.getRandom();

		if (this.damage != oldDamage) {
			for (int index = 0; index < 3; index++) {
				double x = this.player.getRandomX(1);
				double y = this.player.getRandomY() + 0.5;
				double z = this.player.getRandomZ(1);

				double deltaX = random.nextGaussian() * 0.02;
				double deltaY = random.nextGaussian() * 0.02;
				double deltaZ = random.nextGaussian() * 0.02;

				this.player.level().sendParticles(ParticleTypes.HEART, x, y, z, 1, deltaX, deltaY, deltaZ, 1);
			}
		}

		// Apply rain particles
		double x = this.player.getX();
		double y = this.player.getY(0.5);
		double z = this.player.getZ();

		double deltaX = this.player.getBbWidth() / 2;
		double deltaY = this.player.getBbHeight() / 4;
		double deltaZ = this.player.getBbWidth() / 2;

		this.player.level().sendParticles(ParticleTypes.RAIN, x, y, z, RAIN_PARTICLE_COUNT, deltaX, deltaY, deltaZ, 1);

		this.applyDamageRepresentation(this.getDamageProgress());
	}

	public void decrementStainRemovers() {
		if (this.stainRemovers > 0) {
			this.stainRemovers -= 1;
		}
	}

	public boolean isAlive() {
		return this.marker == null;
	}

	public Vec3 getRecoveryPos() {
		return this.isAlive() ? this.player.position() : this.marker.getPos();
	}

	/**
	 * Ticks the player.
	 * @return an elimination result if the player should be eliminated, or {@code null} otherwise
	 */
	public EliminationResult tick() {
		if (this.isAlive()) {
			if (this.phase.getMap().isOutOfBounds(this.player)) {
				return new EliminationResult(this.getOutOfBoundsEliminationMessage(), false);
			} else if (this.getDamageProgress() > 1) {
				Component message = this.lastDamager == null ? this.getGenericEliminationMessage() : this.getDamageEliminationMessage();
				return new EliminationResult(message, true);
			}
		}

		return null;
	}

	public boolean eliminate(EliminationResult result, boolean remove) {
		if (this.phase.isGameEnding()) return false;

		this.phase.sendMessage(result.message());

		if (result.revivable() && this.phase.getConfig().hasRevival()) {
			this.marker = RevivalMarker.spawnFromPlayer(this);
			this.spawnAtRevivalMarker(marker, true);

			return false;
		}

		if (!this.isAlive()) {
			this.marker.destroy();
			this.marker = null;
		}

		if (remove) {
			this.phase.getPlayers().remove(this);
		}
		this.spawn(true);

		return true;
	}

	public void eliminateFromGameRemove(boolean remove) {
		this.eliminate(new EliminationResult(this.getGenericEliminationMessage(), false), remove);
	}

	private Component getOutOfBoundsEliminationMessage() {
		return Component.translatable("text.paintball.eliminated.out_of_bounds", this.player.getDisplayName()).withStyle(ChatFormatting.RED);
	}

	private Component getDamageEliminationMessage() {
		return Component.translatable("text.paintball.eliminated.by", this.player.getDisplayName(), this.lastDamager.getPlayer().getDisplayName()).withStyle(ChatFormatting.RED);
	}

	private Component getGenericEliminationMessage() {
		return Component.translatable("text.paintball.eliminated", this.player.getDisplayName()).withStyle(ChatFormatting.RED);
	}

	@Override
	public String toString() {
		return "PlayerEntry{player=" + this.player + ", team=" + this.team + ", damage=" + this.damage + "}";
	}
}
