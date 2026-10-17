package io.github.haykam821.paintball.game;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.paintball.Main;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.util.valueproviders.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

public record StainRemovalConfig(
        Optional<HolderSet<Item>> items,
        IntProvider initialCount,
        FloatProvider radius,
        Optional<IntProvider> recoverAmount
) {
	public static final StainRemovalConfig DEFAULT = new StainRemovalConfig(
		Optional.empty(),
		ConstantInt.of(3),
		ConstantFloat.of(3.5f),
		Optional.empty()
	);

	public static final Codec<StainRemovalConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			RegistryCodecs.holderSet(Registries.ITEM).optionalFieldOf("items").forGetter(StainRemovalConfig::items),
			IntProviders.NON_NEGATIVE_CODEC.optionalFieldOf("initial_count", DEFAULT.initialCount()).forGetter(StainRemovalConfig::initialCount),
			FloatProviders.codec(0, Float.MAX_VALUE).optionalFieldOf("radius", DEFAULT.radius()).forGetter(StainRemovalConfig::radius),
			IntProviders.NON_NEGATIVE_CODEC.optionalFieldOf("recover_amount").forGetter(StainRemovalConfig::recoverAmount)
		).apply(instance, StainRemovalConfig::new);
	});

	/**
	 * Gets the configured stain removers {@linkplain HolderSet registry entry list}, or a default based on the {@code paintball:stain_removers} tag.
	 */
	public Optional<HolderSet<Item>> getItems(HolderLookup.Provider registries) {
		return this.items.or(() -> registries.lookupOrThrow(Registries.ITEM).get(Main.STAIN_REMOVERS));
	}

	/**
	 * Gives a player the initial item stacks for a stain remover.
	 */
	public void give(ServerPlayer player, int count) {
		RandomSource random = player.getRandom();

		this.getItems(player.registryAccess())
			.flatMap(items -> items.getRandomElement(random))
			.map(Holder::value)
			.ifPresent(item -> {
				ItemStack stack = new ItemStack(item, count);
				player.addItem(stack);
			});
	}

	/**
	 * Calculates the updated damage for a player affected by stain remover, based on their current damage.
	 * @return the amount of damage that the player should be left with
	 */
	public int modifyDamage(ServerPlayer player, int damage) {
		// Empty optional indicates removing all damage
		if (this.recoverAmount().isEmpty()) {
			return 0;
		}

		RandomSource random = player.getRandom();
		return Math.max(0, damage - this.recoverAmount().get().sample(random));
	}
}
