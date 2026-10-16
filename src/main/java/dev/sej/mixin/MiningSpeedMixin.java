package dev.sej.mixin;

import dev.sej.EquipmentData;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MiningToolItem.class)
public class MiningSpeedMixin {
    @Inject(method = "getMiningSpeedMultiplier", at = @At("RETURN"), cancellable = true)
    private void bettabeta$digSpeed(CallbackInfoReturnable<Float> cir) {
        if (cir.getReturnValueF() <= 1.0f) {
            return;
        }
        Float speed = EquipmentData.digSpeed((Item) (Object) this);
        if (speed != null) {
            cir.setReturnValue(speed);
        }
    }

    @Redirect(
            method = "isSuitableFor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ToolMaterial;getMiningLevel()I")
    )
    private int bettabeta$miningLevel(ToolMaterial material) {
        Integer level = EquipmentData.miningLevel((Item) (Object) this);
        return level != null ? level : material.getMiningLevel();
    }
}