package dev.sej.mixin;

import dev.sej.SpiderTweaks;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SpiderEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class SpiderSizeMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void bettabeta$smallerSpiderHitbox(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if ((Object) this instanceof SpiderEntity) {
            cir.setReturnValue(cir.getReturnValue().scaled(SpiderTweaks.SIZE_SCALE));
        }
    }
}