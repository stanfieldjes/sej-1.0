package dev.sej.mixin;

import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreeperEntity.class)
public abstract class CreeperHealthMixin {

    @Inject(method = "createCreeperAttributes", at = @At("RETURN"))
    private static void bettaBetaReduceCreeperHealth(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        DefaultAttributeContainer.Builder attributes = cir.getReturnValue();
        attributes.add(EntityAttributes.GENERIC_MAX_HEALTH, 15.0);
    }
}