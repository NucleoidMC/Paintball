package io.github.haykam821.paintball.game.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import io.github.haykam821.paintball.game.event.LaunchPaintballEvent;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import xyz.nucleoid.stimuli.EventInvokers;
import xyz.nucleoid.stimuli.Stimuli;

public class PaintballLauncherItem extends Item implements PolymerItem {
	private static final int DYE_COLORS = DyeColor.values().length;
	private static final int COOLDOWN = 10;

	public PaintballLauncherItem(Properties settings) {
		super(settings);
	}

	private void playSound(SoundEvent sound, Level level, ServerPlayer player) {
		float pitch = (level.getRandom().nextFloat() * 0.3f) + 1.7f;
		level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.5f, pitch);
	}

	private SoundEvent getFailSound(Level level, ServerPlayer player) {
		return SoundEvents.DISPENSER_FAIL;
	}

	private SoundEvent getLaunchSound(Level level, ServerPlayer player) {
		return SoundEvents.SNOWBALL_THROW;
	}

	private Projectile createDefaultProjectile(Level level, ServerPlayer player) {
		DyeColor color = DyeColor.byFireworkColor(level.getRandom().nextInt(DYE_COLORS));
		if (color == null) {
			color = DyeColor.WHITE;
		}

		Item item = Items.DYE.pick(color);
		if (item == null) {
			item = Items.DYE.white();
		}

		ItemStack stack = new ItemStack(item);
		Snowball projectile = new Snowball(level, player, stack);

		return projectile;
	}

	private Entity createProjectile(Level level, ServerPlayer player) {
		Projectile projectile;

		try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
			projectile = invokers.get(LaunchPaintballEvent.EVENT).onLaunchProjectile(level, player);
		}

		if (projectile == null) {
			projectile = this.createDefaultProjectile(level, player);
		}
		projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5f, 1);

		return projectile;
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		ServerPlayer serverPlayer = (ServerPlayer) user;
		Entity projectile = this.createProjectile(level, serverPlayer);

		if (projectile == null) {
			this.playSound(this.getFailSound(level, serverPlayer), level, serverPlayer);
		} else {
			this.playSound(this.getLaunchSound(level, serverPlayer), level, serverPlayer);
			serverPlayer.awardStat(Stats.ITEM_USED.get(this));

			serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN);

			level.addFreshEntity(projectile);
		}

		return InteractionResult.CONSUME;
	}

	@Override
	public Item getPolymerItem(ItemStack stack, PacketContext context) {
		return Items.DIAMOND_HORSE_ARMOR;
	}

	@Override
	public Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
		return null;
	}
}
