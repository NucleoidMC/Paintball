package io.github.haykam821.paintball.game.phase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.github.haykam821.paintball.game.PaintballConfig;
import io.github.haykam821.paintball.game.event.LaunchPaintballListener;
import io.github.haykam821.paintball.game.map.BlockStaining;
import io.github.haykam821.paintball.game.map.PaintballMap;
import io.github.haykam821.paintball.game.player.PlayerEntry;
import io.github.haykam821.paintball.game.player.WinManager;
import io.github.haykam821.paintball.game.player.armor.ArmorSet;
import io.github.haykam821.paintball.game.player.armor.StainedArmorHelper;
import io.github.haykam821.paintball.game.player.team.TeamEntry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameLogic;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.TeamSelectionLobby;
import xyz.nucleoid.plasmid.game.event.BlockHitListener;
import xyz.nucleoid.plasmid.game.event.EntityHitListener;
import xyz.nucleoid.plasmid.game.event.GameCloseListener;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.PlayerRemoveListener;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.BlockTraversal;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

public class PaintballActivePhase implements BlockHitListener, EntityHitListener, GameCloseListener, GameOpenListener, GameTickListener, LaunchPaintballListener, PlayerAddListener, PlayerDeathListener, PlayerRemoveListener {
	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final PaintballMap map;
	private final PaintballConfig config;
	private final Set<PlayerEntry> players;
	private final Set<TeamEntry> teams;
	private final WinManager winManager = new WinManager(this);
	private final ArmorSet stainedArmorSet;
	private boolean singleplayer;
	private boolean opened;

	public PaintballActivePhase(GameSpace gameSpace, PaintballMap map, TeamSelectionLobby teamSelection, GlobalWidgets widgets, PaintballConfig config) {
		this.world = gameSpace.getWorld();
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;

		this.players = new HashSet<>(this.gameSpace.getPlayerCount());
		this.teams = new HashSet<>(this.config.getTeams().size());
		Map<GameTeam, TeamEntry> gameTeamsToEntries = new HashMap<>(this.config.getTeams().size());

		MinecraftServer server = this.world.getServer();
		ServerScoreboard scoreboard = server.getScoreboard();

		teamSelection.allocate((gameTeam, player) -> {
			// Get or create team
			TeamEntry team = gameTeamsToEntries.get(gameTeam);
			if (team == null) {
				team = new TeamEntry(this, gameTeam, server);
				this.teams.add(team);
				gameTeamsToEntries.put(gameTeam, team);
			}

			this.players.add(new PlayerEntry(this, player, team));
			scoreboard.addPlayerToTeam(player.getEntityName(), team.getScoreboardTeam());
		});

		this.stainedArmorSet = StainedArmorHelper.createArmorSet(this.teams);
	}

	private static void setRules(GameLogic game) {
		game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
		game.setRule(GameRule.BREAK_BLOCKS, RuleResult.DENY);
		game.setRule(GameRule.CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		game.setRule(GameRule.FLUID_FLOW, RuleResult.DENY);
		game.setRule(GameRule.HUNGER, RuleResult.DENY);
		game.setRule(GameRule.INTERACTION, RuleResult.DENY);
		game.setRule(GameRule.MODIFY_ARMOR, RuleResult.DENY);
		game.setRule(GameRule.MODIFY_INVENTORY, RuleResult.DENY);
		game.setRule(GameRule.PLACE_BLOCKS, RuleResult.DENY);
		game.setRule(GameRule.PORTALS, RuleResult.DENY);
		game.setRule(GameRule.PVP, RuleResult.DENY);
		game.setRule(GameRule.TEAM_CHAT, RuleResult.ALLOW);
		game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
	}

	public static void open(GameSpace gameSpace, PaintballMap map, TeamSelectionLobby teamSelection, PaintballConfig config) {
		gameSpace.openGame(game -> {
			GlobalWidgets widgets = new GlobalWidgets(game);
			PaintballActivePhase phase = new PaintballActivePhase(gameSpace, map, teamSelection, widgets, config);

			PaintballActivePhase.setRules(game);

			// Listeners
			game.on(BlockHitListener.EVENT, phase);
			game.on(EntityHitListener.EVENT, phase);
			game.on(GameCloseListener.EVENT, phase);
			game.on(GameOpenListener.EVENT, phase);
			game.on(GameTickListener.EVENT, phase);
			game.on(LaunchPaintballListener.EVENT, phase);
			game.on(PlayerAddListener.EVENT, phase);
			game.on(PlayerDeathListener.EVENT, phase);
			game.on(PlayerRemoveListener.EVENT, phase);
		});
	}

	// Listeners
	@Override
	public ActionResult onBlockHit(ProjectileEntity entity, BlockHitResult hitResult) {
		if (this.config.getStainRadius() > 0) {
			if (entity.getOwner() instanceof ServerPlayerEntity) {
				PlayerEntry entry = this.getPlayerEntry((ServerPlayerEntity) entity.getOwner());
				if (entry != null) {
					DyeColor color = entry.getTeam().getGameTeam().getDye();
					BlockTraversal.create().accept(hitResult.getBlockPos(), (pos, fromPos, depth) -> {
						if (depth > this.config.getStainRadius()) {
							return BlockTraversal.Result.TERMINATE;
						}

						BlockStaining.setStainedState(this.world, pos, color);
						return BlockTraversal.Result.CONTINUE;
					});
				}
			}
		}

		return ActionResult.FAIL;
	}

	@Override
	public ActionResult onEntityHit(ProjectileEntity entity, EntityHitResult hitResult) {
		if (hitResult.getEntity() instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) hitResult.getEntity();

			PlayerEntry entry = this.getPlayerEntry(player);
			if (entry != null) {
				// Ensure players are on different teams
				if (entity.getOwner() instanceof ServerPlayerEntity) {
					ServerPlayerEntity ownerPlayer = (ServerPlayerEntity) entity.getOwner();

					PlayerEntry ownerEntry = this.getPlayerEntry(ownerPlayer);
					if (ownerEntry != null) {
						if (this.config.shouldAllowFriendlyFire() || ownerEntry.getTeam() != entry.getTeam()) {
							entry.damage(ownerEntry);
						}
						return ActionResult.FAIL;
					}
				}

				entry.damage(null);
			}
		}

		return ActionResult.FAIL;
	}

	@Override
	public void onClose() {
		MinecraftServer server = this.world.getServer();
		ServerScoreboard scoreboard = server.getScoreboard();

		for (TeamEntry team : this.teams) {
			scoreboard.removeTeam(team.getScoreboardTeam());
		}
	}

	@Override
	public void onOpen() {
		this.opened = true;
		this.singleplayer = this.players.size() == 1;

		for (PlayerEntry entry : this.players) {
			entry.spawn(false);
		}
	}

	@Override
	public void onTick() {
		Iterator<PlayerEntry> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			PlayerEntry entry = iterator.next();

			Text elimination = entry.tick();
			if (elimination != null) {
				entry.eliminate(elimination, false);
				iterator.remove();
			}
		}

		// Attempt to determine a winner
		Text win = this.winManager.getWin();
		if (win != null) {
			this.sendMessage(win);
			gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	@Override
	public ProjectileEntity onLaunchProjectile(World world, ServerPlayerEntity player) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry == null) return null;

		DyeColor color = entry.getTeam().getGameTeam().getDye();
		if (color == null) {
			color = DyeColor.WHITE;
		}

		Item item = DyeItem.byColor(color);
		if (item == null) {
			item = Items.WHITE_DYE;
		}
		
		SnowballEntity projectile = new SnowballEntity(world, player);
		projectile.setItem(new ItemStack(item));

		return projectile;
	}

	@Override
	public void onAddPlayer(ServerPlayerEntity player) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry == null) {
			player.setGameMode(GameMode.SPECTATOR);
			this.map.teleportToSpectatorSpawn(player);
		} else if (this.opened) {
			entry.eliminate(true);
		}
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		return ActionResult.FAIL;
	}

	@Override
	public void onRemovePlayer(ServerPlayerEntity player) {
		PlayerEntry entry = this.getPlayerEntry(player);
		if (entry != null) {
			entry.eliminate(true);
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
	public void sendMessage(Text message) {
		this.gameSpace.getPlayers().sendMessage(message);
	}

	private PlayerEntry getPlayerEntry(ServerPlayerEntity player) {
		for (PlayerEntry entry : this.players) {
			if (player == entry.getPlayer()) {
				return entry;
			}
		}
		return null;
	}
}