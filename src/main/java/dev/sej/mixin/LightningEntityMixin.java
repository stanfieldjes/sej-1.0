package dev.sej.mixin;

import dev.sej.LightningTorches;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks the moment lightning lands, where vanilla powers a lightning rod. */
@Mixin(LightningEntity.class)
public abstract class LightningEntityMixin {

    @Inject(method = "powerLightningRod", at = @At("HEAD"))
    private void bettabeta$strikeRedstoneTorch(CallbackInfo ci) {
        LightningEntity self = (LightningEntity) (Object) this;
        if (self.getWorld() instanceof ServerWorld world) {
            // Same block vanilla treats as "struck": the one the bolt is standing on.
            BlockPos struck = BlockPos.ofFloored(self.getX(), self.getY() - 1.0E-6, self.getZ());
            LightningTorches.onStrike(world, struck);
        }
    }
}