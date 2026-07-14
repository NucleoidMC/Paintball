package io.github.haykam821.paintball.game.phase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.haykam821.paintball.Main;
import io.github.haykam821.paintball.game.PaintballConfig;
import io.github.haykam821.paintball.game.StainRemovalConfig;
import io.github.haykam821.paintball.game.event.LaunchPaintballEvent;
import io.github.haykam821.paintball.game.map.BlockStaining;
import io.github.haykam821.paintball.game.map.PaintballMap;
import io.github.haykam821.paintball.game.player.EliminationResult;
import io.github.haykam821.paintball.game.player.PlayerEntry;
import io.github.haykam821.paintball.game.player.WinManager;
import io.github.haykam821.paintball.game.player.armor.ArmorSet;
import io.github.haykam821.paintball.game.player.armor.StainedArmorHelper;
import io.github.haykam821.paintball.game.player.team.TeamEntry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.HolderSet;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamChat;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.util.BlockTraversal;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.player.PlayerSpectateEntityEvent;
import xyz.nucleoid.stimuli.event.projectile.ProjectileHitEvent;

public class PaintballActivePhase implements ProjectileHitEvent.Block, ProjectileHitEvent.Entity, GameActivityEvents.Enable, GameActivityEvents.Tick, LaunchPaintballEvent, GamePlayerEvents.Accept, ItemUseEvent, PlayerDeathEvent, PlayerSpectateEntityEvent, GamePlayerEvents.Remove {
	private final ServerLevel level;
	private final GameSpace gameSpace;
	private final PaintballMap map;
	private final PaintballConfig config;
	private final Set<PlayerEntry> players;
	private final Map<GameTeamKey, TeamEntry> teams = new HashMap<>();
	private final TeamManager teamManager;
	private final WinManager winManager = new WinManager(this);
	private final ArmorSet stainedArmorSet;
	private boolean singleplayer;
	private int ticksUntilClose = -1;

	public PaintballActivePhase(GameSpace gameSpace, ServerLevel level, PaintballMap map, TeamManager teamManager, GlobalWidgets widgets, PaintballConfig config) {
		this.level = level;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;

		PlayerSet participants = this.gameSpace.getPlayers().participants();

		this.players = new HashSet<>(participants.size());
		this.teamManager = teamManager;

		for (ServerPlayer player : participants) {
			GameTeamKey teamKey = this.teamManager.teamFor(player);
			
			TeamEntry teamEntry = this.teams.get(teamKey);
			if (teamEntry == null) {
				teamEntry = new TeamEntry(this, teamKey, this.teamManager.getTeamConfig(teamKey));
				this.teams.put(teamKey, teamEntry);
			}
			
			this.players.add(new PlayerEntry(this, player, teamEntry));
		}

		this.stainedArmorSet = StainedArmorHelper.createArmorSet(this.teams.values());
	}

	private static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.BREAK_BLOCKS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.FLUID_FLOW);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.MODIFY_ARMOR);
		activity.deny(GameRuleType.MODIFY_INVENTORY);
		activity.deny(GameRuleType.PLACE_BLOCKS);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(Main.PROJECTILE_BARRIER_COLLISION);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.SWAP_OFFHAND);
		activity.deny(GameRuleType.THROW_ITEMS);
		activity.deny(GameRuleType.USE_BLOCKS);
		activity.deny(GameRuleType.USE_ENTITIES);
	}

	public static void open(GameSpace gameSpace, ServerLevel level, PaintballMap map, TeamSelectionLobby teamSelection, PaintballConfig config) {
		gameSpace.setActivity(activity -> {
			TeamManager teamManager = TeamManager.addTo(activity);
			TeamChat.addTo(activity, teamManager);

			for (GameTeam team : config.getTeams()) {
				GameTeamConfig teamConfig = GameTeamConfig.builder(team.config())
					.setFriendlyFire(false)
					.setCollision(PlayerTeam.CollisionRule.NEVER)
					.build();

				teamManager.addTeam(team.key(), teamConfig);
			}

			teamSelection.allocate(gameSpace.getPlayers().participants(), (teamKey, player) -> {
				teamManager.addPlayerTo(player, teamKey);
			});

			GlobalWidgets widgets = GlobalWidgets.addTo(activity);
			PaintballActivePhase phase = new PaintballActivePhase(gameSpace, level, map, teamManager, widgets, config);

			PaintballActivePhase.setRules(activity);

			// Listeners
			activity.listen(ProjectileHitEvent.BLOCK, phase);
			activity.listen(ProjectileHitEvent.ENTITY, phase);
			activity.listen(GameActivityEvents.ENABLE, phase);
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(LaunchPaintballEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.ACCEPT, phase);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
			activity.listen(ItemUseEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(PlayerSpectateEntityEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
		});
	}

	// Listeners
	@Override
	public EventResult onHitBlock(Projectile entity, BlockHitResult hitResult) {
		if (!this.tryApplyStainRemoval(hitResult.getLocation(), entity) && this.config.getStainRadius() > 0) {
			if (entity.getOwner() instanceof ServerPlayer) {
				PlayerEntry entry = this.getPlayerEntry((ServerPlayer) entity.getOwner());
				if (entry != null) {
					DyeColor color = entry.getTeam().getConfig().blockDyeColor();
					BlockTraversal.create().accept(hitResult.getBlockPos(), (pos, fromPos, depth) -> {
						if (depth > this.config.getStainRadius()) {
							return BlockTraversal.Result.TERMINATE;
						}

						BlockStaining.setStainedState(this.level, pos, color);
						return BlockTraversal.Result.CONTINUE;
					});
				}
			}
		}

		return EventResult.DENY;
	}

	@Override
	public EventResult onHitEntity(Projectile entity, EntityHitResult hitResult) {
		if (!this.tryApplyStainRemoval(hitResult.getLocation(), entity) && hitResult.getEntity() instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) hitResult.getEntity();

			PlayerEntry entry = this.getPlayerEntry(player);
			if (entry != null) {
				// Ensure players are on different teams
				if (entity.getOwner() instanceof ServerPlayer) {
					ServerPlayer ownerPlayer = (ServerPlayer) entity.getOwner();

					PlayerEntry ownerEntry = this.getPlayerEntry(ownerPlayer);
					if (ownerEntry != null) {
						if (this.config.shouldAllowFriendlyFire() || ownerEntry.getTeam() != entry.getTeam()) {
							entry.damage(ownerEntry);
						}
						return EventResult.DENY;
					}
				}

				entry.damage(null);
			}
		}

		return EventResult.DENY;
	}

	@Override
	public void onEnable() {
		this.singleplayer = this.players.size() == 1;

		for (PlayerEntry entry : this.players) {
			entry.spawn(false);
		}

		for (ServerPlayer player : this.gameSpace.getPlayers().spectators()) {
			this.map.teleportToSpectatorSpawn(player);
			player.setGameMode(GameType.SPECTATOR);
		}
	}

	@Override
	public void onTick() {
		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			return;
		}

		Iterator<PlayerEntry> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			PlayerEntry entry = iterator.next();

			EliminationResult result = entry.tick();
			if (result != null && entry.eliminate(result, false)) {
				iterator.remove();
			}
		}

		// Attempt to determine a winner
		Component win = this.winManager.getWin();
		if (win != null) {
			this.sendMessage(win);
			this.gameSpace.getPlayers().playSound(SoundEvents.PLAYER_LEVELUP, SoundSource.UI, 1, 1);
			this.ticksUntilClose = this.config.getTicksUntilClose().sample(this.level.getRandom());
		}
	}

	@Override
	public Projectile onLaunchProjectile(Level level, ServerPlayer player) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry == null) return null;

		DyeColor color = entry.getTeam().getConfig().blockDyeColor();
		if (color == null) {
			color = DyeColor.WHITE;
		}

		Item item = Items.DYE.pick(color);
		if (item == null) {
			item = Items.DYE.white();
		}
		
		ItemStack stack = new ItemStack(item);
		Snowball projectile = new Snowball(level, player, stack);

		return projectile;
	}

	@Override
	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return this.map.acceptSpectatorJoins(acceptor, this.level).thenRunForEach(player -> {
			player.setGameMode(GameType.SPECTATOR);
		});
	}
	
	@Override
	public InteractionResult onUse(ServerPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		PlayerEntry entry = this.getPlayerEntry(player);

		if (entry != null && this.isStainRemovalStack(stack)) {
			entry.decrementStainRemovers();
		}

		return InteractionResult.PASS;
	}

	@Override
	public EventResult onDeath(ServerPlayer player, DamageSource source) {
		return EventResult.DENY;
	}

	@Override
	public EventResult onSpectateEntity(ServerPlayer player, Entity target) {
		return target instanceof ArmorStand ? EventResult.DENY : EventResult.PASS;
	}

	@Override
	public void onRemovePlayer(ServerPlayer player) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry != null) {
			entry.eliminateFromGameRemove(true);
		}
	}

	// Getters
	public PaintballMap getMap() {
		return this.map;
	}

	public PaintballConfig getConfig() {
		return this.config;
	}

	public Set<PlayerEntry> getPlayers() {
		return this.players;
	}

	public boolean isSingleplayer() {
		return this.singleplayer;
	}

	public ArmorSet getStainedArmorSet() {
		return this.stainedArmorSet;
	}

	// Utilities
	public boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}

	public void sendMessage(Component message) {
		this.gameSpace.getPlayers().sendMessage(message);
	}

	private boolean isStainRemovalStack(ItemStack stack) {
		StainRemovalConfig config = this.getConfig().getStainRemoval();

		Optional<HolderSet<Item>> items = config.getItems(this.level.registryAccess());
		if (items.isEmpty()) return false;

		return items.get().contains(stack.typeHolder());
	}

	/**
	 * @return whether this projectile acted as a stain remover
	 */
	private boolean tryApplyStainRemoval(Vec3 pos, Projectile projectile) {
		StainRemovalConfig config = this.getConfig().getStainRemoval();

		if (!(projectile instanceof AbstractThrownPotion potion)) return false;
		if (!this.isStainRemovalStack(potion.getItem())) return false;

		if (!(projectile.getOwner() instanceof ServerPlayer player)) return false;

		PlayerEntry owner = this.getPlayerEntry(player);
		if (owner == null) return false;

		float radius = config.radius().sample(this.level.getRandom());

		for (PlayerEntry entry : this.players) {
			if (owner.getTeam() == entry.getTeam() && entry.getRecoveryPos().distanceTo(pos) <= radius) {
				entry.recover(config);
			}
		}

		return true;
	}

	private PlayerEntry getPlayerEntry(ServerPlayer player) {
		for (PlayerEntry entry : this.players) {
			if (player == entry.getPlayer()) {
				return entry;
			}
		}
		return null;
	}
}