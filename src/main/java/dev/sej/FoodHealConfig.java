package dev.sej;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

/** How much health each food restores, all at once when it's eaten (2 health = 1 heart). */
public class FoodHealConfig {

    public static final Map<Item, Float> FOOD_HEALTH = new HashMap<>();

    static {
        FOOD_HEALTH.put(Items.APPLE, 4.0F);
        FOOD_HEALTH.put(Items.BREAD, 6.0F);
        FOOD_HEALTH.put(Items.COD, 4.0F);
        FOOD_HEALTH.put(Items.PORKCHOP, 5.0F);
        FOOD_HEALTH.put(Items.COOKED_COD, 8.0F);
        FOOD_HEALTH.put(Items.MUSHROOM_STEW, 8.0F);
        FOOD_HEALTH.put(Items.COOKED_PORKCHOP, 10.0F);
        FOOD_HEALTH.put(Items.GOLDEN_APPLE, 20.0F); // full heal
    }

    public static boolean isFood(Item item) {
        return FOOD_HEALTH.containsKey(item);
    }

    /** Health restored by this item, or null if it isn't one of the pack's foods. */
    public static Float healthOf(Item item) {
        return FOOD_HEALTH.get(item);
    }
}