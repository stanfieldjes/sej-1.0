package dev.sej.mixin;

import dev.sej.EquipmentData;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemBarMixin {

    @Inject(method = "getItemBarStep", at = @At("HEAD"), cancellable = true)
    private void bettabeta$barStep(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        Integer max = EquipmentData.maxDamage(self.getItem());
        if (max == null || max <= 0) {
            return;
        }
        int step = Math.round(13.0F - (float) self.getDamage() * 13.0F / (float) max);
        cir.setReturnValue(MathHelper.clamp(step, 0, 13));
    }

    @Inject(method = "getItemBarColor", at = @At("HEAD"), cancellable = true)
    private void bettabeta$barColor(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        Integer max = EquipmentData.maxDamage(self.getItem());
        if (max == null || max <= 0) {
            return;
        }
        float remaining = Math.max(0.0F, ((float) max - (float) self.getDamage()) / (float) max);
        cir.setReturnValue(MathHelper.hsvToRgb(remaining / 3.0F, 1.0F, 1.0F));
    }
}