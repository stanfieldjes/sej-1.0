package dev.sej.mixin;

import dev.sej.EquipmentData;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class EquipmentDurabilityMixin {

    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void bettabeta$maxDamage(CallbackInfoReturnable<Integer> cir) {
        Integer max = EquipmentData.maxDamage((Item) (Object) this);
        if (max != null) {
            cir.setReturnValue(max);
        }
    }
}