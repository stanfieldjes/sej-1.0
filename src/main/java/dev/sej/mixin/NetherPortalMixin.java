package dev.sej.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.dimension.NetherPortal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(NetherPortal.class)
public abstract class NetherPortalMixin {

    @Inject(method = "getNewPortal", at = @At("HEAD"), cancellable = true)
    private static void bettabeta$noNewPortals(WorldAccess world, BlockPos pos, Direction.Axis axis,
                                               CallbackInfoReturnable<Optional<NetherPortal>> cir) {
        cir.setReturnValue(Optional.empty());
    }
}