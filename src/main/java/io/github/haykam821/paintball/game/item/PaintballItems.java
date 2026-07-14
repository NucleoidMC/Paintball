package io.github.haykam821.paintball.game.item;

import java.util.function.Function;

import eu.pb4.sgui.api.SguiUtils;
import io.github.haykam821.paintball.Main;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public enum PaintballItems implements ItemLike {
	PAINTBALL_LAUNCHER("paintball_launcher", settings -> {
		settings.stacksTo(1);
		return new PaintballLauncherItem(settings);
	}),

	STAIN_REMOVER("stain_remover", settings -> {
		settings.component(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));

		// Prevent using the custom translation key format used by potions
		Identifier id = Main.identifier("stain_remover");
		String translationKey = Util.makeDescriptionId("item", id);

		Component customName = Component.translatable(translationKey).withStyle(SguiUtils.STYLE_CLEARER);
		settings.component(DataComponents.CUSTOM_NAME, customName);

		return new StainRemoverItem(settings);
	});

	private final ResourceKey<Item> key;
	private final Item item;

	private PaintballItems(String path, Function<Item.Properties, Item> factory) {
		Identifier id = Main.identifier(path);
		this.key = ResourceKey.create(Registries.ITEM, id);

		this.item = factory.apply(new Item.Properties().setId(key));
	}

	@Override
	public Item asItem() {
		return this.item;
	}

	public static void register() {
		for (PaintballItems item : PaintballItems.values()) {
			Registry.register(BuiltInRegistries.ITEM, item.key, item.item);
		}
	}
}
