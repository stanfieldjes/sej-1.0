package dev.sej.mixin;

import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BubbleColumnBlock.class)
public class BubbleColumnBlockMixin {

    @Inject(method = "update*", at = @At("HEAD"), cancellable = true)
    private static void cancelUpdate(WorldAccess world, BlockPos pos, BlockState state, CallbackInfo ci) {
        ci.cancel();
    }
}