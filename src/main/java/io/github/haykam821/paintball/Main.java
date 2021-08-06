package io.github.haykam821.paintball;

import io.github.haykam821.paintball.game.PaintballConfig;
import io.github.haykam821.paintball.game.item.PaintballItems;
import io.github.haykam821.paintball.game.phase.PaintballWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	public static final String MOD_ID = "paintball";

	private static final Identifier PAINTBALL_ID = new Identifier(MOD_ID, "paintball");
	public static final GameType<PaintballConfig> PAINTBALL_TYPE = GameType.register(PAINTBALL_ID, PaintballWaitingPhase::open, PaintballConfig.CODEC);

	@Override
	public void onInitialize() {
		PaintballItems.register();
	}
}
