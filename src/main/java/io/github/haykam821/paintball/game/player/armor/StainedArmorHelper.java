package io.github.haykam821.paintball.game.player.armor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.haykam821.paintball.game.player.team.TeamEntry;
import net.minecraft.item.DyeItem;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;

public final class StainedArmorHelper {
	private static final int DEFAULT_STAINED_COLOR = 0xAD5398;

	public static ArmorSet createArmorSet(Set<TeamEntry> teams) {
		return new ArmorSet(StainedArmorHelper.getColor(teams));
	}

	private static int getColor(Set<TeamEntry> teams) {
		if (teams.size() < 2) {
			return DEFAULT_STAINED_COLOR;
		}

		List<DyeItem> dyes = new ArrayList<>(teams.size());

		for (TeamEntry team : teams) {
			DyeColor color = team.getGameTeam().getDye();
			if (color == null) continue;

			DyeItem dye = DyeItem.byColor(color);
			if (dye == null) continue;

			dyes.add(dye);
		}

		return StainedArmorHelper.getColorFromDyes(dyes);
	}

	private static int getColorFromDyes(List<DyeItem> dyes) {
		ItemStack stack = DyeableItem.blendAndSetColor(new ItemStack(Items.LEATHER_BOOTS), dyes);
		return ((DyeableItem) Items.LEATHER_BOOTS).getColor(stack);
	}
}
