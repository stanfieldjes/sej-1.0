package dev.sej.mixin;

import dev.sej.ArmorCalculator;
import dev.sej.DamageCategory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private static final DamageCategory[] CATEGORIES = {
            DamageCategory.BLAST, DamageCategory.COMBAT,
            DamageCategory.FIRE, DamageCategory.FALL
    };

    @Inject(
            method = "applyArmorToDamage(Lnet/minecraft/entity/damage/DamageSource;F)F",
            at = @At("HEAD"),
            cancellable = true
    )
    private void neutralizeVanillaArmorForPlayers(DamageSource source, float amount,
                                                  CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof PlayerEntity) {
            cir.setReturnValue(amount);
        }
    }

    @Inject(method = "getArmor", at = @At("HEAD"), cancellable = true)
    private void bettabeta$customArmorValue(CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof PlayerEntity player)) {
            cir.setReturnValue(0);
            return;
        }

        float totalReduction = 0f;
        for (DamageCategory category : CATEGORIES) {
            totalReduction += 1.0f - ArmorCalculator.computeDamageMultiplier(player, category);
        }

        int armorPoints = Math.round(totalReduction / 3.0f * 20.0f);
        armorPoints = Math.max(0, Math.min(20, armorPoints));

        cir.setReturnValue(armorPoints);
    }
}