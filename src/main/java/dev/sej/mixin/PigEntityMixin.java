package dev.sej.mixin;

import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PigEntity.class)
public abstract class PigEntityMixin {

    @Inject(method = "onStruckByLightning", at = @At("HEAD"), cancellable = true)
    private void bettabeta$noZombifiedPiglin(ServerWorld world, LightningEntity lightning, CallbackInfo ci) {
        PigEntity pig = (PigEntity) (Object) this;

        pig.setFireTicks(pig.getFireTicks() + 1);
        if (pig.getFireTicks() == 0) {
            pig.setOnFireFor(8);
        }
        pig.damage(pig.getDamageSources().lightningBolt(), 5.0F);

        ci.cancel();
    }
}