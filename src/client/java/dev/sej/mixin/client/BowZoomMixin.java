package dev.sej.mixin.client;

import dev.sej.ArrowDrawInfo;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class BowZoomMixin {

    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void applyCustomCurveZoom(CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity)(Object)this;
        if (!self.isUsingItem() || !self.getActiveItem().isOf(Items.BOW)) return;

        int ticksHeld = self.getItemUseTime();
        float effectiveTicks;
        float s1 = (float) ArrowDrawInfo.STAGE_1_TICKS;

        if (ticksHeld <= s1) {
            effectiveTicks = (float) ticksHeld;
        }
        else if (ticksHeld <= s1 + (float) ArrowDrawInfo.STAGE_DELAY_TICKS) {
            effectiveTicks = s1;
        }
        else {
            effectiveTicks = (float) (ticksHeld - ArrowDrawInfo.STAGE_DELAY_TICKS);
            effectiveTicks = Math.min(effectiveTicks, s1 + (float) ArrowDrawInfo.STAGE_2_TICKS);
        }

        float totalActiveTicks = s1 + (float) ArrowDrawInfo.STAGE_2_TICKS;
        float progress = effectiveTicks / totalActiveTicks;

        float curve = (float) Math.pow(progress, ArrowDrawInfo.ZOOM_EXPONENT);
        cir.setReturnValue(1.0f - (curve * ArrowDrawInfo.MAX_ZOOM));
    }
}