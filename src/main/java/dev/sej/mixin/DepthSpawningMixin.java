package dev.sej.mixin;

import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class DepthSpawningMixin {

    @Inject(
            method = "canSpawn(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/entity/SpawnReason;)Z",
            at = @At("RETURN"),
            cancellable = true
    )

    @SuppressWarnings("ConstantConditions")
    private void bettaBetaAdjustSurfaceSpawns(
            WorldAccess world,
            SpawnReason spawnReason,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!((Object) this instanceof HostileEntity entity)) {
            return;
        }

        if (spawnReason != SpawnReason.NATURAL || !cir.getReturnValueZ()) {
            return;
        }

        float currentY = (float) entity.getY();
        float surfaceY = 64.0f;
        float bottomY = 0.0f;

        float maxCancelChance = 0.5f;

        if (currentY >= surfaceY) {
            if (entity.getRandom().nextFloat() < maxCancelChance) {
                cir.setReturnValue(false);
            }
            return;
        }

        if (currentY <= bottomY) {
            return;
        }

        float depthRatio = currentY / surfaceY;
        float cancelChance = depthRatio * maxCancelChance;

        if (entity.getRandom().nextFloat() < cancelChance) {
            cir.setReturnValue(false);
        }
    }
}