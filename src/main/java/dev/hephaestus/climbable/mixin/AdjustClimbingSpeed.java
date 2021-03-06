package dev.hephaestus.climbable.mixin;

import dev.hephaestus.climbable.api.ClimbingSpeedRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.tag.Tag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class AdjustClimbingSpeed extends Entity {
	@Shadow private Optional<BlockPos> climbingPos;

	public AdjustClimbingSpeed(EntityType<?> type, World world) {
		super(type, world);
	}

	@Redirect(method = "isClimbing", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;isIn(Lnet/minecraft/tag/Tag;)Z"))
	private boolean isClimbable(Block block, Tag<Block> tag) {
		return ClimbingSpeedRegistry.canClimb(block);
	}

	@Inject(method = "isClimbing", at = @At("RETURN"), cancellable = true)
	private void allowClimbingOfSolidObjects(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) {
			BlockPos pos = this.getBlockPos().offset(this.getMovementDirection());
			Block block = this.getEntityWorld().getBlockState(pos).getBlock();
			if (this.horizontalCollision && ClimbingSpeedRegistry.canClimb(block)) {
				this.climbingPos = Optional.of(pos);
				cir.setReturnValue(true);
			}
		}
	}

	@ModifyConstant(method = "applyClimbingSpeed", constant = @Constant(doubleValue = 0.15000000596046448D))
	private double modifyMaxSpeed(double speed) {
		return adjustClimbingSpeed(speed);
	}

	@ModifyConstant(method = "applyClimbingSpeed", constant = @Constant(doubleValue = -0.15000000596046448D))
	private double modifyMinSpeed(double speed) {
		return this.adjustClimbingSpeed(speed);
	}

	@ModifyConstant(method = "method_26318", constant = @Constant(doubleValue = 0.2D))
	private double modifyMoveSpeed(double speed) {
		return this.adjustClimbingSpeed(speed);
	}

	@Unique
	private double adjustClimbingSpeed(double speed) {
		if (this.climbingPos.isPresent()) {
			Block block = this.world.getBlockState(this.climbingPos.get()).getBlock();
			speed = speed * ClimbingSpeedRegistry.getModifier(block);
		}

		return speed;
	}
}
