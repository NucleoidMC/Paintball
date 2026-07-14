package io.github.haykam821.paintball.game.player;

import net.minecraft.world.entity.EntityTypes;
import org.joml.Matrix4f;

import com.mojang.authlib.GameProfile;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import io.github.haykam821.paintball.mixin.ArmorStandAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Rotations;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RevivalMarker {
	private static final Vec3 HEAD_DISPLAY_OFFSET = new Vec3(0, 1.975, 0);

	private final ArmorStand bodyEntity;
	private final HolderAttachment attachment;

	private RevivalMarker(ArmorStand bodyEntity, HolderAttachment attachment) {
		this.bodyEntity = bodyEntity;
		this.attachment = attachment;
	}

	public Vec3 getPos() {
		return this.bodyEntity.position();
	}

	public float getPitch() {
		return this.bodyEntity.getXRot();
	}

	public float getYaw() {
		return this.bodyEntity.getYRot();
	}

	public void destroy() {
		this.bodyEntity.discard();

		this.attachment.holder().destroy();
		this.attachment.destroy();
	}

	private static ArmorStand createArmorStand(PlayerEntry entry) {
		ServerPlayer player = entry.getPlayer();
		ServerLevel world = player.level();

		ArmorStand entity = new ArmorStand(EntityTypes.ARMOR_STAND, world);
		ArmorStandAccessor accessor = (ArmorStandAccessor) (Object) entity;

		// Position
		entity.setPos(entry.getRecoveryPos());

		entity.setYRot(player.getYRot());
		entity.setXRot(player.getXRot());

		// Appearance
		entity.setHeadPose(new Rotations(player.getXRot(), 0, 0));

		entity.setShowArms(true);
		entity.setNoBasePlate(true);

		// Behavior
		accessor.paintball$setDisabledSlots(Integer.MAX_VALUE);
		entity.setInvulnerable(true);

		return entity;
	}

	private static ItemStack createHeadStack(ServerPlayer player) {
		GameProfile profile = player.getGameProfile();

		ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
		stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));

		return stack;
	}

	public static RevivalMarker spawnFromPlayer(PlayerEntry entry) {
		ServerPlayer player = entry.getPlayer();
		ServerLevel level = player.level();

		// Body entity
		ArmorStand bodyEntity = createArmorStand(entry);

		entry.applyArmor(entry.getDamageProgress(), bodyEntity);

		ItemStack mainHandStack = player.getItemInHand(InteractionHand.MAIN_HAND);
		ItemStack offHandStack = player.getItemInHand(InteractionHand.OFF_HAND);

		if (player.getMainArm() == HumanoidArm.RIGHT) {
			bodyEntity.setItemInHand(InteractionHand.MAIN_HAND, mainHandStack);
			bodyEntity.setItemInHand(InteractionHand.OFF_HAND, offHandStack);
		} else {
			bodyEntity.setItemInHand(InteractionHand.MAIN_HAND, offHandStack);
			bodyEntity.setItemInHand(InteractionHand.OFF_HAND, mainHandStack);
		}

		bodyEntity.setCustomName(player.getDisplayName());
		bodyEntity.setCustomNameVisible(true);

		// Head element
		ItemDisplayElement headElement = new ItemDisplayElement(createHeadStack(player));

		Matrix4f transformation = new Matrix4f()
			.translate(0, -0.55f, 0)
			.rotateY(Mth.PI)
			.rotateY(-player.getYRot() * Mth.DEG_TO_RAD)
			.rotateX(-player.getXRot() * Mth.DEG_TO_RAD)
			.translate(0, 0.5f, 0);

		headElement.setTransformation(transformation);

		headElement.setOffset(HEAD_DISPLAY_OFFSET);
		headElement.setSendPositionUpdates(false);

		headElement.setDisplayWidth(1);
		headElement.setDisplayHeight(1);
		headElement.setViewRange(4);

		headElement.setInvisible(true);

		// Element holder
		ElementHolder holder = new ElementHolder();
		holder.addElement(headElement);

		level.addFreshEntity(bodyEntity);
		VirtualEntityUtils.addVirtualPassenger(bodyEntity, headElement.getEntityId());

		HolderAttachment attachment = EntityAttachment.ofTicking(holder, bodyEntity);

		return new RevivalMarker(bodyEntity, attachment);
	}
}
