package dev.sej.mixin;

import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityFreezeOnHurtMixin {

    @Inject(method = "mobTick", at = @At("HEAD"), cancellable = true)
    private void freezeAiWhileHurt(CallbackInfo ci) {
        MobEntity self = (MobEntity)(Object)this;
        if (self.hurtTime > 0) {
            ci.cancel();
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void zeroVelocityWhileHurt(CallbackInfo ci) {
        MobEntity self = (MobEntity)(Object)this;
        if (self.hurtTime > 0) {
            self.setVelocity(0, self.getVelocity().y, 0);
        }
    }
}