package dev.sej.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class MossyCobblestoneTickMixin {

    @Shadow protected abstract BlockState asBlockState();

    @Inject(method = "hasRandomTicks", at = @At("HEAD"), cancellable = true)
    private void forceMossyRandomTicks(CallbackInfoReturnable<Boolean> cir) {
        if (this.asBlockState().isOf(Blocks.MOSSY_COBBLESTONE)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "randomTick", at = @At("HEAD"))
    private void onMossyRandomTick(ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (this.asBlockState().isOf(Blocks.MOSSY_COBBLESTONE)) {

            // Pick a random target block within the 3-block radius (7x7x7 area)
            int rx = random.nextInt(7) - 3;
            int ry = random.nextInt(7) - 3;
            int rz = random.nextInt(7) - 3;

            BlockPos targetPos = pos.add(rx, ry, rz);
            BlockState targetState = world.getBlockState(targetPos);

            if (targetState.isOf(Blocks.COBBLESTONE)) {

                // Rule 1: Ensure the target is adjacent/diagonal to at least one mossy cobblestone
                boolean touchesMoss = false;
                Iterable<BlockPos> targetNeighbors = BlockPos.iterate(targetPos.add(-1, -1, -1), targetPos.add(1, 1, 1));
                for (BlockPos neighbor : targetNeighbors) {
                    if (world.getBlockState(neighbor).isOf(Blocks.MOSSY_COBBLESTONE)) {
                        touchesMoss = true;
                        break;
                    }
                }

                if (!touchesMoss) {
                    return;
                }

                // Rule 2: Check target environment (Water vs Air)
                boolean targetTouchesWater = false;
                boolean targetTouchesAir = false;
                for (Direction dir : Direction.values()) {
                    BlockState faceState = world.getBlockState(targetPos.offset(dir));

                    // Ignore water directly below the block so it visually makes sense
                    if (dir != Direction.DOWN && faceState.getFluidState().isOf(Fluids.WATER)) {
                        targetTouchesWater = true;
                    } else if (faceState.isAir()) {
                        targetTouchesAir = true;
                    }
                }

                // THE ANCHOR RULE: Check if the ticking source block touches water
                boolean tickingTouchesWater = false;
                for (Direction dir : Direction.values()) {
                    if (dir != Direction.DOWN && world.getFluidState(pos.offset(dir)).isOf(Fluids.WATER)) {
                        tickingTouchesWater = true;
                        break;
                    }
                }

                // Break the infinite chain. If NEITHER block touches water, abort the spread.
                if (!tickingTouchesWater && !targetTouchesWater) {
                    return;
                }

                // If touching water, convert immediately (bypasses percentage rule)
                if (targetTouchesWater) {
                    world.setBlockState(targetPos, Blocks.MOSSY_COBBLESTONE.getDefaultState());
                }
                // If touching air (and not water), evaluate the 75% rule
                else if (targetTouchesAir) {

                    int airMossyCount = 0;
                    int airCobbleCount = 0;

                    // Scan the 3-block radius (7x7x7 area) for the ratio math
                    Iterable<BlockPos> areaPos = BlockPos.iterate(pos.add(-3, -3, -3), pos.add(3, 3, 3));
                    for (BlockPos checkPos : areaPos) {
                        BlockState checkState = world.getBlockState(checkPos);
                        boolean isMossy = checkState.isOf(Blocks.MOSSY_COBBLESTONE);
                        boolean isCobble = checkState.isOf(Blocks.COBBLESTONE);

                        // Only count Mossy and normal Cobblestone
                        if (isMossy || isCobble) {
                            boolean cTouchesWater = false;
                            boolean cTouchesAir = false;

                            for (Direction dir : Direction.values()) {
                                BlockState neighbor = world.getBlockState(checkPos.offset(dir));

                                if (dir != Direction.DOWN && neighbor.getFluidState().isOf(Fluids.WATER)) {
                                    cTouchesWater = true;
                                } else if (neighbor.isAir()) {
                                    cTouchesAir = true;
                                }
                            }

                            // Only blocks touching air and NOT touching water count towards the percentage
                            if (!cTouchesWater && cTouchesAir) {
                                if (isMossy) {
                                    airMossyCount++;
                                } else {
                                    airCobbleCount++;
                                }
                            }
                        }
                    }

                    int totalAirBlocks = airMossyCount + airCobbleCount;
                    if (totalAirBlocks > 0) {
                        // The + 1 represents the hypothetical conversion of the target block
                        float newRatio = (airMossyCount + 1.0f) / totalAirBlocks;

                        if (newRatio <= 0.75f) {
                            world.setBlockState(targetPos, Blocks.MOSSY_COBBLESTONE.getDefaultState());
                        }
                    }
                }
            }
        }
    }
}