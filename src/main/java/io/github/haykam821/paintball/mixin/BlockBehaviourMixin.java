package io.github.haykam821.paintball.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.haykam821.paintball.Main;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.BarrierBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
	@Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
	public void removePaintballBarrierCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> ci) {
		if (!(state.getBlock() instanceof BarrierBlock)) return;

		if (!(context instanceof EntityCollisionContext entityContext)) return;
		if (!(entityContext.getEntity() instanceof Projectile entity)) return;

		GameSpace gameSpace = GameSpaceManager.get().byLevel(entity.level());
		if (gameSpace == null) return;
		if (gameSpace.getBehavior().testRule(Main.PROJECTILE_BARRIER_COLLISION) != EventResult.DENY) return;

		ci.setReturnValue(Shapes.empty());
	}
}
