package dev.sej.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GrassBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds Cornflower to bonemeal output
 */
@Mixin(GrassBlock.class)
public class GrassBlockMixin {

    @Inject(method = "grow", at = @At("TAIL"))
    private void addCornflower(ServerWorld world, Random random, BlockPos pos,
                               BlockState state, CallbackInfo ci) {
        for (int i = 0; i < 128; i++) {
            BlockPos target = pos.up();

            for (int j = 0; j < i / 16; j++) {
                target = target.add(
                        random.nextInt(3) - 1,
                        (random.nextInt(3) - 1) * random.nextInt(3) / 2,
                        random.nextInt(3) - 1
                );
            }

            if (world.getBlockState(target).isOf(Blocks.AIR) &&
                    world.getBlockState(target.down()).isOf(Blocks.GRASS_BLOCK)) {

                if (random.nextInt(8) == 0) { // ~12.5% of valid spots
                    world.setBlockState(target, Blocks.CORNFLOWER.getDefaultState(), 3);
                }
            }
        }
    }
}