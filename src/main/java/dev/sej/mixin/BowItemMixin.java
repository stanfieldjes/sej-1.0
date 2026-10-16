package dev.sej.mixin;

import dev.sej.ArrowDrawInfo;
import net.minecraft.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BowItem.class)
public class BowItemMixin {

    @Redirect(
            method = "onStoppedUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BowItem;getPullProgress(I)F")
    )
    private float getDelayedTwoStagePullProgress(int useTicks) {
        float ticks = (float) useTicks;
        float s1 = (float) ArrowDrawInfo.STAGE_1_TICKS;
        float delay = s1 + (float) ArrowDrawInfo.STAGE_DELAY_TICKS;

        if (ticks <= s1) {
            float progress = ticks / s1;
            ArrowDrawInfo.lastPotentialDamage = progress * ArrowDrawInfo.STAGE_1_DAMAGE;
            ArrowDrawInfo.lastIntendedVelocity = progress * ArrowDrawInfo.STAGE_1_VELOCITY;
            return ArrowDrawInfo.lastIntendedVelocity / 3.0f;
        }

        if (ticks <= delay) {
            ArrowDrawInfo.lastPotentialDamage = ArrowDrawInfo.STAGE_1_DAMAGE;
            ArrowDrawInfo.lastIntendedVelocity = ArrowDrawInfo.STAGE_1_VELOCITY;
            return ArrowDrawInfo.lastIntendedVelocity / 3.0f;
        }

        float s2Ticks = Math.min(ticks - delay, (float) ArrowDrawInfo.STAGE_2_TICKS);
        float progress = s2Ticks / (float) ArrowDrawInfo.STAGE_2_TICKS;

        ArrowDrawInfo.lastPotentialDamage = ArrowDrawInfo.STAGE_1_DAMAGE + (progress * (ArrowDrawInfo.STAGE_2_DAMAGE - ArrowDrawInfo.STAGE_1_DAMAGE));
        ArrowDrawInfo.lastIntendedVelocity = ArrowDrawInfo.STAGE_1_VELOCITY + (progress * (ArrowDrawInfo.STAGE_2_VELOCITY - ArrowDrawInfo.STAGE_1_VELOCITY));

        return ArrowDrawInfo.lastIntendedVelocity / 3.0f;
    }
}