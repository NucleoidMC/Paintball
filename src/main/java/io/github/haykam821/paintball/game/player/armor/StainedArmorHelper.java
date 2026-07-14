package io.github.haykam821.paintball.game.player.armor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.haykam821.paintball.game.player.team.TeamEntry;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;

public final class StainedArmorHelper {
	private static final int DEFAULT_STAINED_COLOR = 0xAD5398;

	public static ArmorSet createArmorSet(Collection<TeamEntry> teams) {
		return new ArmorSet(StainedArmorHelper.getColor(teams));
	}

	private static int getColor(Collection<TeamEntry> teams) {
		if (teams.size() < 2) {
			return DEFAULT_STAINED_COLOR;
		}

		List<DyeColor> dyes = new ArrayList<>(teams.size());

		for (TeamEntry team : teams) {
			DyeColor color = team.getConfig().blockDyeColor();
			if (color == null) continue;
			dyes.add(color);
		}

		return StainedArmorHelper.getColorFromDyes(dyes);
	}

	private static int getColorFromDyes(List<DyeColor> dyes) {
		ItemStack stack = DyedItemColor.applyDyes(new ItemStack(Items.LEATHER_BOOTS), dyes);
		return DyedItemColor.getOrDefault(stack, DyedItemColor.LEATHER_COLOR);
	}
}
