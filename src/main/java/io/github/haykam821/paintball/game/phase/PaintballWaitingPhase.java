package io.github.haykam821.paintball.game.phase;

import io.github.haykam821.paintball.game.PaintballConfig;
import io.github.haykam821.paintball.game.map.PaintballMap;
import io.github.haykam821.paintball.game.map.PaintballMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameLogic;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.TeamSelectionLobby;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDamageListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;

public class PaintballWaitingPhase implements GameTickListener, PlayerAddListener, PlayerDamageListener, PlayerDeathListener, RequestStartListener {
	private final GameSpace gameSpace;
	private final PaintballMap map;
	private final TeamSelectionLobby teamSelection;
	private final PaintballConfig config;

	public PaintballWaitingPhase(GameSpace gameSpace, PaintballMap map, TeamSelectionLobby teamSelection, PaintballConfig config) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.teamSelection = teamSelection;
		this.config = config;
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
		game.setRule(GameRule.TEAM_CHAT, RuleResult.DENY);
		game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
	}

	public static GameOpenProcedure open(GameOpenContext<PaintballConfig> context) {
		PaintballConfig config = context.getConfig();

		PaintballMapBuilder mapBuilder = new PaintballMapBuilder(config);
		PaintballMap map = mapBuilder.create();

		BubbleWorldConfig worldConfig = new BubbleWorldConfig()
			.setGenerator(map.createGenerator(context.getServer()))
			.setGameRule(GameRules.NATURAL_REGENERATION, false)
			.setGameRule(GameRules.DROWNING_DAMAGE, false)
			.setDefaultGameMode(GameMode.ADVENTURE);

		return context.createOpenProcedure(worldConfig, game -> {
			TeamSelectionLobby teamSelection = TeamSelectionLobby.applyTo(game, config.getTeams());
			PaintballWaitingPhase phase = new PaintballWaitingPhase(game.getGameSpace(), map, teamSelection, config);
			GameWaitingLobby.applyTo(game, config.getPlayerConfig());

			PaintballWaitingPhase.setRules(game);

			// Listeners
			game.listen(GameTickListener.EVENT, phase);
			game.listen(PlayerAddListener.EVENT, phase);
			game.listen(PlayerDamageListener.EVENT, phase);
			game.listen(PlayerDeathListener.EVENT, phase);
			game.listen(RequestStartListener.EVENT, phase);
		});
	}

	@Override
	public void onTick() {
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			if (this.map.isOutOfBounds(player)) {
				this.map.teleportToWaitingSpawn(player);
			}
		}
	}

	@Override
	public void onAddPlayer(ServerPlayerEntity player) {
		this.map.teleportToWaitingSpawn(player);
	}

	@Override
	public ActionResult onDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return ActionResult.FAIL;
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		this.map.teleportToWaitingSpawn(player);
		return ActionResult.FAIL;
	}

	@Override
	public StartResult requestStart() {
		PaintballActivePhase.open(this.gameSpace, this.map, this.teamSelection, this.config);
		return StartResult.OK;
	}
}