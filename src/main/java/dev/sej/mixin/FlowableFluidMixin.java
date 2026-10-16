package dev.sej.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowableFluid.class)
public class FlowableFluidMixin {

    @Shadow public FluidState getUpdatedState(World world, BlockPos pos, BlockState state) { return null; }
    @Shadow public void tryFlow(World world, BlockPos pos, FluidState state) {}

    @Inject(method = "onScheduledTick", at = @At("HEAD"), cancellable = true)
    private void keepFallingWater(World world, BlockPos pos, FluidState state, CallbackInfo ci) {
        if (!state.getFluid().matchesType(Fluids.WATER)) return;
        if (state.isStill()) return;
        if (!state.get(FlowableFluid.FALLING)) return;

        FluidState updated = getUpdatedState(world, pos, world.getBlockState(pos));
        if (!updated.isEmpty()) return;

        ci.cancel();
        FluidState below = world.getFluidState(pos.down());
        if (below.getFluid().matchesType(Fluids.WATER) && below.get(FlowableFluid.FALLING)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        } else if (world.getBlockState(pos.down()).isAir()) {
            tryFlow(world, pos, state);
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        } else {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
    }
}