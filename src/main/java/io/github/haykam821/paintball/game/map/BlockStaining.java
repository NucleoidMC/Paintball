package io.github.haykam821.paintball.game.map;

import io.github.haykam821.paintball.Main;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.util.ColoredBlocks;

public final class BlockStaining {
	private BlockStaining() {
		return;
	}

	private static <T extends Comparable<T>> BlockState copyProperty(BlockState source, BlockState target, Property<T> property) {
		return target.with(property, source.get(property));
	}

	private static Block getStainedBlock(Block block, DyeColor color) {
		if (block.isIn(Main.STAINABLE_TERRACOTTA)) {
			return ColoredBlocks.terracotta(color);
		}
		return block;
	}

	private static BlockState getStainedState(BlockState state, DyeColor color) {
		Block stainedBlock = BlockStaining.getStainedBlock(state.getBlock(), color);
		BlockState stainedState = stainedBlock.getDefaultState();

		for (Property<?> property : state.getProperties()) {
			if (stainedState.contains(property)) {
				stainedState = BlockStaining.copyProperty(state, stainedState, property);
			}
		}
	
		return stainedState;
	}

	public static void setStainedState(World world, BlockPos pos, DyeColor color) {
		BlockState state = world.getBlockState(pos);
		world.setBlockState(pos, BlockStaining.getStainedState(state, color));
	}
}
