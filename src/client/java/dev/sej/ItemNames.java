package dev.sej;

import java.util.HashMap;
import java.util.Map;

public final class ItemNames {
    public static final Map<String, String> NAMES = new HashMap<>();
    static {
        NAMES.put("block.minecraft.red_bed", "Bed");
        NAMES.put("item.minecraft.cod", "Raw Fish");
        NAMES.put("item.minecraft.cooked_cod", "Cooked Fish");
        NAMES.put("item.minecraft.oak_boat", "Boat");
        NAMES.put("item.minecraft.green_dye", "Green Dye");
        NAMES.put("item.minecraft.red_dye", "Red Dye");
        NAMES.put("item.minecraft.yellow_dye", "Yellow Dye");
        NAMES.put("block.minecraft.oak_planks", "Planks");
        NAMES.put("block.minecraft.dandelion", "Dandelion");
    }

    private ItemNames() {}

    /** The loaded translations with our names laid on top. */
    public static Map<String, String> apply(Map<String, String> loaded) {
        Map<String, String> merged = new HashMap<>(loaded);
        merged.putAll(NAMES);
        return merged;
    }
}