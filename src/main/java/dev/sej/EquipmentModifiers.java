package dev.sej;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.ModifyItemAttributeModifiersCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;

import java.util.UUID;

public class EquipmentModifiers implements ModInitializer {

    private static final UUID CUSTOM_WEAPON_DAMAGE_ID =
            UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");

    @Override
    public void onInitialize() {
        ModifyItemAttributeModifiersCallback.EVENT.register((stack, slot, modifiers) -> {
            Item item = stack.getItem();

            if (slot == EquipmentSlot.MAINHAND) {
                Float damage = EquipmentData.damage(item);
                if (damage != null) {
                    modifiers.removeAll(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                    modifiers.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(
                            CUSTOM_WEAPON_DAMAGE_ID,
                            "Sej Attack Damage",
                            damage - EquipmentData.FIST_DAMAGE,
                            EntityAttributeModifier.Operation.ADDITION
                    ));
                }
            }

            if (item instanceof ArmorItem armorItem && armorItem.getSlotType() == slot) {
                modifiers.removeAll(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
            }
        });
    }
}