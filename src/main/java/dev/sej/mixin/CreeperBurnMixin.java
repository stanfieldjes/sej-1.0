package dev.sej.mixin;

import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public class CreeperBurnMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void burnInDaylight(CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity) (Object) this;

        if (!creeper.getWorld().isClient && creeper.isAlive()) {
            if (((MobEntityInvoker) creeper).invokeIsAffectedByDaylight()) {
                creeper.setOnFireFor(8);
            }
        }
    }
}