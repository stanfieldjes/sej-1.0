package dev.sej.mixin;

import dev.sej.LightningTorches;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * When a storm picks a spot for lightning and there's no lightning rod nearby,
 * the nearest redstone torch within 128 blocks takes the strike instead. Same rules
 * as a vanilla rod: the torch needs open sky above it, and it has to be raining there.
 */
@Mixin(ServerWorld.class)
public abstract class ServerWorldLightningMixin {

    @Inject(method = "getLightningRodPos", at = @At("RETURN"), cancellable = true)
    private void bettabeta$redstoneTorchRods(BlockPos pos, CallbackInfoReturnable<Optional<BlockPos>> cir) {
        if (cir.getReturnValue().isPresent()) return;

        ServerWorld world = (ServerWorld) (Object) this;
        world.getPointOfInterestStorage().getNearestPosition(
                type -> type.matchesKey(LightningTorches.POI_KEY),
                torch -> torch.getY() == world.getTopY(Heightmap.Type.WORLD_SURFACE, torch.getX(), torch.getZ()) - 1,
                pos, 128, PointOfInterestStorage.OccupationStatus.ANY
        ).ifPresent(torch -> cir.setReturnValue(Optional.of(torch.up())));
    }
}