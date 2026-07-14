package io.github.haykam821.paintball.game.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.resources.Identifier;

public class StainRemoverItem extends SplashPotionItem implements PolymerItem {
	public StainRemoverItem(Item.Properties settings) {
		super(settings);
	}

	@Override
	public Item getPolymerItem(ItemStack stack, PacketContext context) {
		return Items.SPLASH_POTION;
	}

	@Override
	public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
		return null;
	}
}
