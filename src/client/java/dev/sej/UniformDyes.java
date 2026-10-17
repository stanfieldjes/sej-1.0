package dev.sej;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public final class UniformDyes {

    /** Tint for each dye (0xRRGGBB). Change a value to recolour that dye. */
    public static final Map<Item, Integer> COLORS = new LinkedHashMap<>();
    static {
        COLORS.put(Items.WHITE_DYE, 0xF2F2F2);
        COLORS.put(Items.LIGHT_GRAY_DYE, 0xBCBCC0);
        COLORS.put(Items.GRAY_DYE, 0x6A6A70);
        COLORS.put(Items.BLACK_DYE, 0x404046);
        COLORS.put(Items.BROWN_DYE, 0x8A5630);
        COLORS.put(Items.RED_DYE, 0xC83A32);
        COLORS.put(Items.ORANGE_DYE, 0xF28E32);
        COLORS.put(Items.YELLOW_DYE, 0xF0D834);
        COLORS.put(Items.LIME_DYE, 0x80D040);
        COLORS.put(Items.GREEN_DYE, 0x5A7A24);
        COLORS.put(Items.CYAN_DYE, 0x2C96A8);
        COLORS.put(Items.LIGHT_BLUE_DYE, 0x7CA4F0);
        COLORS.put(Items.BLUE_DYE, 0x3448C0);
        COLORS.put(Items.PURPLE_DYE, 0x8C40C8);
        COLORS.put(Items.MAGENTA_DYE, 0xD45CD8);
        COLORS.put(Items.PINK_DYE, 0xF090B4);
    }

    private static final String MODEL_JSON =
            "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"sej:item/dye_powder\"}}";

    private UniformDyes() {}

    /** Call from the client entrypoint. */
    public static void register() {
        Set<Identifier> dyeIds = new HashSet<>();
        for (Item dye : COLORS.keySet()) dyeIds.add(Registries.ITEM.getId(dye));

        ModelLoadingPlugin.register(plugin -> plugin.modifyModelOnLoad().register((model, context) -> {
            if (context.id() instanceof ModelIdentifier id && "inventory".equals(id.getVariant())
                    && dyeIds.contains(new Identifier(id.getNamespace(), id.getPath()))) {
                JsonUnbakedModel dye = JsonUnbakedModel.deserialize(MODEL_JSON);
                dye.id = id.toString();
                return dye;
            }
            return model;
        }));

        ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> tintIndex == 0 ? COLORS.getOrDefault(stack.getItem(), 0xFFFFFF) : 0xFFFFFF,
                COLORS.keySet().toArray(new ItemConvertible[0]));
    }
}