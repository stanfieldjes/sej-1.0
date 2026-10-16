package dev.sej;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import org.jetbrains.annotations.Nullable;

public final class ArmorCalculator {

    private ArmorCalculator() {}

    /**
     * Reduction value used for a material's strength category.
     * Every material gets the same value here — the "specialty" is which
     * category it applies to, not how big the number is. This is also the
     * highest value in the whole system, so it doubles as the maximum
     * possible reduction for any single damage category (see
     * {@link #computeDamageMultiplier} for why that bound always holds).
     */
    public static final float MAX_STRENGTH_VALUE = 0.9f;

    /**
     * A material's weakness value is its base multiplied by this.
     */
    public static final float WEAKNESS_MULTIPLIER = 1f;

    /**
     * Base reduction value per material, used for any category that is
     * neither the material's strength nor its weakness.
     */
    public static float materialBase(ArmorMaterials material) {
        if (material == ArmorMaterials.LEATHER) return 0.15f;
        if (material == ArmorMaterials.CHAIN)   return 0.3f;
        if (material == ArmorMaterials.GOLD)    return 0.3f;
        if (material == ArmorMaterials.IRON)    return 0.6f;
        if (material == ArmorMaterials.DIAMOND) return 0.7f;
        return 0f;
    }

    /**
     * The one damage category each material is strong against, or null if
     * it has none (iron).
     */
    public static @Nullable DamageCategory materialStrength(ArmorMaterials material) {
        if (material == ArmorMaterials.LEATHER) return DamageCategory.FALL;
        if (material == ArmorMaterials.CHAIN)   return DamageCategory.COMBAT;
        if (material == ArmorMaterials.GOLD)    return DamageCategory.FIRE;
        if (material == ArmorMaterials.DIAMOND) return DamageCategory.BLAST;
        return null;
    }

    /**
     * The one damage category each material is weak against, or null if
     * it has none (iron).
     */
    public static @Nullable DamageCategory materialWeakness(ArmorMaterials material) {
        if (material == ArmorMaterials.LEATHER) return DamageCategory.BLAST;
        if (material == ArmorMaterials.CHAIN)   return DamageCategory.FIRE;
        if (material == ArmorMaterials.GOLD)    return DamageCategory.COMBAT;
        if (material == ArmorMaterials.DIAMOND) return DamageCategory.FALL;
        return null;
    }

    /**
     * Reduction value a material contributes for a given category:
     * MAX_STRENGTH_VALUE if this is the material's strength,
     * base * WEAKNESS_MULTIPLIER if this is the material's weakness,
     * base otherwise.
     */
    public static float categoryValue(ArmorMaterials material, DamageCategory category) {
        float base = materialBase(material);
        if (materialStrength(material) == category) return MAX_STRENGTH_VALUE;
        if (materialWeakness(material) == category) return base * WEAKNESS_MULTIPLIER;
        return base;
    }

    /**
     * Fraction of a piece's reduction assigned to each damage category.
     * Each column sums to 1.00 across the four slots, so a full suit
     * always applies a total weight of exactly 1.0 to every category.
     */
    public static float pieceWeight(EquipmentSlot slot, DamageCategory category) {
        return switch (slot) {
            case HEAD  -> switch (category) { case BLAST -> ((float) 1/2); case COMBAT -> ((float) 1/3); case FIRE -> 0f;   case FALL -> 0f;   case OTHER -> 0f; };
            case CHEST -> switch (category) { case BLAST -> ((float) 1/2); case COMBAT -> ((float) 1/2); case FIRE -> ((float) 1/6); case FALL -> 0f;   case OTHER -> 0f; };
            case LEGS  -> switch (category) { case BLAST -> 0f;    case COMBAT -> ((float) 1/6); case FIRE -> ((float) 1/2); case FALL -> ((float) 1/2); case OTHER -> 0f; };
            case FEET  -> switch (category) { case BLAST -> 0f;    case COMBAT -> 0f;    case FIRE -> ((float) 1/3); case FALL -> ((float) 1/2); case OTHER -> 0f; };
            default    -> 0f;
        };
    }

    public static DamageCategory classify(DamageSource source) {
        if (source.isIn(DamageTypeTags.IS_EXPLOSION))  return DamageCategory.BLAST;
        if (source.isIn(DamageTypeTags.IS_FIRE))       return DamageCategory.FIRE;
        if (source.isIn(DamageTypeTags.IS_FALL))       return DamageCategory.FALL;
        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) return DamageCategory.COMBAT;
        if (source.getAttacker() != null
                && !source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            return DamageCategory.COMBAT;
        }
        return DamageCategory.OTHER;
    }

    /**
     * Returns a damage multiplier in [1 - MAX_STRENGTH_VALUE, 1.0].
     * Per piece: reduction += pieceWeight(slot, category) * categoryValue(material, category)
     *
     * No explicit cap is applied. It isn't needed: pieceWeight for a given
     * category always sums to 1.0 across the four slots, and categoryValue
     * never exceeds MAX_STRENGTH_VALUE, so totalReduction is mathematically
     * bounded by MAX_STRENGTH_VALUE regardless of what's equipped.
     */
    public static float computeDamageMultiplier(PlayerEntity player, DamageCategory category) {
        if (category == DamageCategory.OTHER) return 1.0f;

        float totalReduction = 0f;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {

            float weight = pieceWeight(slot, category);
            if (weight == 0f) continue;

            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ArmorItem armor)) continue;

            ArmorMaterials material = getArmorMaterials(armor);
            if (material == null) continue;

            totalReduction += weight * categoryValue(material, category);
        }

        return 1.0f - totalReduction;
    }

    public static @Nullable ArmorMaterials getArmorMaterials(ArmorItem armor) {
        if (armor.getMaterial() == ArmorMaterials.LEATHER) return ArmorMaterials.LEATHER;
        if (armor.getMaterial() == ArmorMaterials.CHAIN)   return ArmorMaterials.CHAIN;
        if (armor.getMaterial() == ArmorMaterials.GOLD)    return ArmorMaterials.GOLD;
        if (armor.getMaterial() == ArmorMaterials.IRON)    return ArmorMaterials.IRON;
        if (armor.getMaterial() == ArmorMaterials.DIAMOND) return ArmorMaterials.DIAMOND;
        return null;
    }
}