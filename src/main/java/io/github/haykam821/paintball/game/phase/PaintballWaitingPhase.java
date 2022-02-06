package io.github.haykam821.paintball.game.phase;

import io.github.haykam821.paintball.game.PaintballConfig;
import io.github.haykam821.paintball.game.map.PaintballMap;
import io.github.haykam821.paintball.game.map.PaintballMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class PaintballWaitingPhase implements GameActivityEvents.Tick, GamePlayerEvents.Offer, PlayerDamageEvent, PlayerDeathEvent, GameActivityEvents.RequestStart {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final PaintballMap map;
	private final TeamSelectionLobby teamSelection;
	private final PaintballConfig config;

	public PaintballWaitingPhase(GameSpace gameSpace, ServerWorld world, PaintballMap map, TeamSelectionLobby teamSelection, PaintballConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.teamSelection = teamSelection;
		this.config = config;
	}

	private static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.BREAK_BLOCKS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.FLUID_FLOW);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.INTERACTION);
		activity.deny(GameRuleType.MODIFY_ARMOR);
		activity.deny(GameRuleType.MODIFY_INVENTORY);
		activity.deny(GameRuleType.PLACE_BLOCKS);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.THROW_ITEMS);
	}

	public static GameOpenProcedure open(GameOpenContext<PaintballConfig> context) {
		PaintballConfig config = context.config();
		MinecraftServer server = context.server();

		PaintballMapBuilder mapBuilder = new PaintballMapBuilder(config);
		PaintballMap map = mapBuilder.create(server);

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(server))
			.setGameRule(GameRules.NATURAL_REGENERATION, false)
			.setGameRule(GameRules.DROWNING_DAMAGE, false);

		return context.openWithWorld(worldConfig, (activity, world) -> {
			TeamSelectionLobby teamSelection = TeamSelectionLobby.addTo(activity, config.getTeams());
			PaintballWaitingPhase phase = new PaintballWaitingPhase(activity.getGameSpace(), world, map, teamSelection, config);
			GameWaitingLobby.addTo(activity, config.getPlayerConfig());

			PaintballWaitingPhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(GamePlayerEvents.OFFER, phase);
			activity.listen(PlayerDamageEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GameActivityEvents.REQUEST_START, phase);
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
	public PlayerOfferResult onOfferPlayer(PlayerOffer offer) {
		return this.map.acceptWaitingSpawnOffer(offer, this.world).and(() -> {
			offer.player().changeGameMode(GameMode.ADVENTURE);
		});
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
	public GameResult onRequestStart() {
		PaintballActivePhase.open(this.gameSpace, this.world, this.map, this.teamSelection, this.config);
		return GameResult.ok();
	}
}