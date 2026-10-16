package dev.sej.mixin;

import dev.sej.ArmorCalculator;
import dev.sej.DamageCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public class ArmorFireTickMixin {

    @ModifyVariable(
            method = "setOnFireFor",
            at = @At("HEAD"),
            argsOnly = true
    )
    private int scaleFireDuration(int seconds) {
        Entity self = (Entity) (Object) this;

        // Only apply on the Server side for Players
        if (self instanceof PlayerEntity player && !player.getWorld().isClient()) {
            float multiplier = ArmorCalculator.computeDamageMultiplier(player, DamageCategory.FIRE);
            return (int) (seconds * multiplier);
        }

        return seconds;
    }
}