package io.github.haykam821.paintball.game.map;

import io.github.haykam821.paintball.Main;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import xyz.nucleoid.plasmid.api.util.ColoredBlocks;

public final class BlockStaining {
	private BlockStaining() {
		return;
	}

	private static BlockState getStainedState(BlockState state, DyeColor color) {
		if (state.is(Main.STAINABLE_TERRACOTTA)) {
			Block stainedBlock = ColoredBlocks.terracotta(color);
			return stainedBlock.withPropertiesOf(state);
		} else {
			return state;
		}
	}

	public static void setStainedState(Level level, BlockPos pos, DyeColor color) {
		BlockState state = level.getBlockState(pos);
		level.setBlockAndUpdate(pos, BlockStaining.getStainedState(state, color));
	}
}
