package dev.sej.mixin;

import dev.sej.ArmorCalculator;
import dev.sej.DamageCategory;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerArmorMixin {

    @Unique
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    @ModifyVariable(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float applyCustomArmorReduction(float amount, DamageSource source) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        DamageCategory category = ArmorCalculator.classify(source);
        return amount * ArmorCalculator.computeDamageMultiplier(self, category);
    }

    @Inject(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD")
    )
    private void applyWeightedDurabilityDamage(DamageSource source, float amount,
                                               CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        if (self.hurtTime > 0) return;

        DamageCategory category = ArmorCalculator.classify(source);

        EquipmentSlot[] slots = new EquipmentSlot[SLOTS.length];
        float[] weights = new float[SLOTS.length];
        int count = 0;
        float totalWeight = 0f;

        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = self.getEquippedStack(slot);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ArmorItem armor)) continue;
            if (ArmorCalculator.getArmorMaterials(armor) == null) continue;

            float weight = bettabeta$categoryWeight(slot, category);
            if (weight <= 0f) continue;

            slots[count] = slot;
            weights[count] = weight;
            totalWeight += weight;
            count++;
        }

        if (count == 0 || totalWeight <= 0f) return;

        int durabilityLoss = Math.max(1, (int) Math.floor(amount));

        for (int i = 0; i < durabilityLoss; i++) {
            EquipmentSlot chosen = bettabeta$pickWeightedSlot(self, slots, weights, count, totalWeight);

            ItemStack stack = self.getEquippedStack(chosen);
            if (stack.isEmpty()) continue;

            stack.damage(1, self, p -> p.sendEquipmentBreakStatus(chosen));
        }
    }

    @Unique
    private float bettabeta$categoryWeight(EquipmentSlot slot, DamageCategory category) {
        if (category == DamageCategory.OTHER) return 0.25f;
        return ArmorCalculator.pieceWeight(slot, category);
    }

    @Unique
    private EquipmentSlot bettabeta$pickWeightedSlot(PlayerEntity self, EquipmentSlot[] slots,
                                                     float[] weights, int count, float totalWeight) {
        float roll = self.getRandom().nextFloat() * totalWeight;
        float cumulative = 0f;
        for (int s = 0; s < count; s++) {
            cumulative += weights[s];
            if (roll < cumulative) return slots[s];
        }
        return slots[count - 1];
    }
}