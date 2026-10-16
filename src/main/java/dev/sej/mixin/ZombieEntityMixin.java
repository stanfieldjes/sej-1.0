package dev.sej.mixin;

import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.ZombieEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieEntity.class)
public class ZombieEntityMixin {
    @Inject(method = "canConvertInWater", at = @At("HEAD"), cancellable = true)
    private void bettaBeta$disableDrownedConversion(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(
            method = "setBaby",
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventSetBaby(boolean baby, CallbackInfo ci) {
        if (baby) {
            ci.cancel();
        }
    }

    @Inject(method = "createZombieAttributes", at = @At("RETURN"))
    private static void bettaBetaIncreaseZombieSpeed(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        DefaultAttributeContainer.Builder attributes = cir.getReturnValue();
        attributes.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0);
    }
}