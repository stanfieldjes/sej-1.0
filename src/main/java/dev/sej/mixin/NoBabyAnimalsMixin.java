package dev.sej.mixin;

import net.minecraft.entity.passive.PassiveEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PassiveEntity.class)
public abstract class NoBabyAnimalsMixin {

    @Inject(method = "setBreedingAge", at = @At("HEAD"), cancellable = true)
    private void bettaBetaPreventBabyAnimals(int age, CallbackInfo ci) {
        if (age < 0) {
            ci.cancel();
        }
    }
}