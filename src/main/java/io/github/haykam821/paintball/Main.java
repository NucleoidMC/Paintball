package io.github.haykam821.paintball;

import io.github.haykam821.paintball.game.PaintballConfig;
import io.github.haykam821.paintball.game.item.PaintballItems;
import io.github.haykam821.paintball.game.phase.PaintballWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.block.Block;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	public static final String MOD_ID = "paintball";

	private static final Identifier PAINTBALL_ID = new Identifier(MOD_ID, "paintball");
	public static final GameType<PaintballConfig> PAINTBALL_TYPE = GameType.register(PAINTBALL_ID, PaintballWaitingPhase::open, PaintballConfig.CODEC);

	private static final Identifier STAINABLE_TERRACOTTA_ID = new Identifier(MOD_ID, "stainable_terracotta");
	public static final Tag<Block> STAINABLE_TERRACOTTA = TagRegistry.block(STAINABLE_TERRACOTTA_ID);

	@Override
	public void onInitialize() {
		PaintballItems.register();
	}
}
