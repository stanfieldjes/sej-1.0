package dev.sej.mixin;

import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.SpiderEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpiderEntity.class)
public class SpiderEntityMixin {
    @Inject(method = "createSpiderAttributes", at = @At("RETURN"))
    private static void bettaBetaDecreaseSpiderHealth(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        DefaultAttributeContainer.Builder attributes = cir.getReturnValue();
        attributes.add(EntityAttributes.GENERIC_MAX_HEALTH, 15);
        attributes.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35);
        attributes.add(EntityAttributes.GENERIC_ATTACK_SPEED, 10);
    }
}