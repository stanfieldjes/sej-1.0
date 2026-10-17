package dev.sej;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;


public final class ItemTextureSwaps {

    public static final Map<Identifier, String> SWAPS = new HashMap<>();
    static {
        SWAPS.put(new Identifier("minecraft", "cooked_porkchop"), "minecraft:item/cooked_beef");
    }

    private ItemTextureSwaps() {}

    public static void register() {
        ModelLoadingPlugin.register(plugin -> plugin.modifyModelOnLoad().register((model, context) -> {
            if (context.id() instanceof ModelIdentifier id && "inventory".equals(id.getVariant())) {
                String texture = SWAPS.get(new Identifier(id.getNamespace(), id.getPath()));
                if (texture != null) {
                    JsonUnbakedModel swapped = JsonUnbakedModel.deserialize(
                            "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + texture + "\"}}");
                    swapped.id = id.toString();
                    return swapped;
                }
            }
            return model;
        }));
    }
}