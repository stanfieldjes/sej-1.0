package dev.sej;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class EquipmentData {

    public static final float FIST_DAMAGE = 1.0f;

    public enum Shape { SWORD, AXE, PICKAXE, SHOVEL, HOE }

    public enum Tier {
        //                                  level  durab  speed | sword  axe  pick  shovel  hoe
        WOOD   (ToolMaterials.WOOD,     0,     64,    3f,    4f,   3f,   2f,    2f,   1f),
        STONE  (ToolMaterials.STONE,    1,    128,    4f,    5f,   4f,   3f,    3f,   2f),
        GOLD   (ToolMaterials.GOLD,     2,     64,    5f,    5f,   4f,   3f,    3f,   2f),
        IRON   (ToolMaterials.IRON,     2,    512,    6f,  7.5f,   5f,   4f,    4f,   3f),
        DIAMOND(ToolMaterials.DIAMOND,  3,   2048,   10f,   10f,   6f,   5f,    5f,   4f);

        public final ToolMaterial material;
        public final int miningLevel;
        public final int durability;
        public final float digSpeed;
        private final Map<Shape, Float> damageByShape;

        Tier(ToolMaterial material, int miningLevel, int durability, float digSpeed,
             float sword, float axe, float pickaxe, float shovel, float hoe) {
            this.material = material;
            this.miningLevel = miningLevel;
            this.durability = durability;
            this.digSpeed = digSpeed;

            Map<Shape, Float> map = new EnumMap<>(Shape.class);
            map.put(Shape.SWORD, sword);
            map.put(Shape.AXE, axe);
            map.put(Shape.PICKAXE, pickaxe);
            map.put(Shape.SHOVEL, shovel);
            map.put(Shape.HOE, hoe);
            this.damageByShape = map;
        }

        public float damage(Shape shape) {
            return damageByShape.get(shape);
        }
    }

    public enum ArmorTier {
        LEATHER  (ArmorMaterials.LEATHER,  64),
        CHAIN    (ArmorMaterials.CHAIN,   128),
        GOLD     (ArmorMaterials.GOLD,    64),
        IRON     (ArmorMaterials.IRON,    256),
        DIAMOND  (ArmorMaterials.DIAMOND, 512);

        public final ArmorMaterial material;
        public final int durability;

        ArmorTier(ArmorMaterial material, int durability) {
            this.material = material;
            this.durability = durability;
        }
    }

    public static Shape shapeOf(Item item) {
        if (item instanceof SwordItem)   return Shape.SWORD;
        if (item instanceof AxeItem)     return Shape.AXE;
        if (item instanceof PickaxeItem) return Shape.PICKAXE;
        if (item instanceof ShovelItem)  return Shape.SHOVEL;
        if (item instanceof HoeItem)     return Shape.HOE;
        return null;
    }

    public static Tier tierOf(Item item) {
        return item instanceof ToolItem tool ? BY_TOOL_MATERIAL.get(tool.getMaterial()) : null;
    }

    public static ArmorTier armorTierOf(Item item) {
        return item instanceof ArmorItem armor ? BY_ARMOR_MATERIAL.get(armor.getMaterial()) : null;
    }

    public static Float damage(Item item) {
        Tier tier = tierOf(item);
        Shape shape = shapeOf(item);
        return tier == null || shape == null ? null : tier.damage(shape);
    }

    public static Float digSpeed(Item item) {
        Tier tier = tierOf(item);
        Shape shape = shapeOf(item);
        if (tier == null || shape == null || shape == Shape.SWORD) {
            return null;
        }
        return tier.digSpeed;
    }

    public static Integer miningLevel(Item item) {
        Tier tier = tierOf(item);
        Shape shape = shapeOf(item);
        if (tier == null || shape == null || shape == Shape.SWORD) {
            return null;
        }
        return tier.miningLevel;
    }

    public static Integer maxDamage(Item item) {
        Integer misc = MiscHolder.DURABILITY.get(item);
        if (misc != null) {
            return misc;
        }

        Tier tier = tierOf(item);
        if (tier != null && shapeOf(item) != null) {
            return tier.durability;
        }

        ArmorTier armor = armorTierOf(item);
        return armor == null ? null : armor.durability;
    }

    public static boolean isTracked(Item item) {
        return maxDamage(item) != null || damage(item) != null;
    }

    private static final Map<ToolMaterial, Tier> BY_TOOL_MATERIAL = new HashMap<>();
    private static final Map<ArmorMaterial, ArmorTier> BY_ARMOR_MATERIAL = new HashMap<>();

    static {
        for (Tier tier : Tier.values()) {
            BY_TOOL_MATERIAL.put(tier.material, tier);
        }
        for (ArmorTier tier : ArmorTier.values()) {
            BY_ARMOR_MATERIAL.put(tier.material, tier);
        }
    }

    private static final class MiscHolder {
        static final Map<Item, Integer> DURABILITY = Map.of(
                Items.BOW,              256,
                Items.FISHING_ROD,       64,
                Items.FLINT_AND_STEEL,   64,
                Items.SHEARS,           256
        );
    }

    private EquipmentData() {}
}