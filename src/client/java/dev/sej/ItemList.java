package dev.sej;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the creative item list from assets/sej/item_list.json.
 *
 * Every array in the file is read in order (keys starting with "_" are skipped),
 * so sections can be added or reordered freely. Because it's a client resource,
 * kubejs/assets/sej/item_list.json overrides the copy in the jar and
 * F3+T reloads it.
 */
public class ItemList implements SimpleSynchronousResourceReloadListener {

    private static final Identifier FILE = new Identifier("sej", "item_list.json");
    private static List<ItemStack> items = List.of();

    public static List<ItemStack> get() {
        return items;
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier("sej", "item_list");
    }

    @Override
    public void reload(ResourceManager manager) {
        List<ItemStack> loaded = new ArrayList<>();
        Optional<Resource> resource = manager.getResource(FILE);

        if (resource.isEmpty()) {
            Sej.LOGGER.warn("Item list {} not found, creative item list will be empty", FILE);
        } else {
            try (Reader reader = resource.get().getReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> section : root.entrySet()) {
                    if (section.getKey().startsWith("_") || !section.getValue().isJsonArray()) continue;
                    readSection(section.getKey(), section.getValue().getAsJsonArray(), loaded);
                }
            } catch (Exception e) {
                Sej.LOGGER.error("Failed to read item list {}", FILE, e);
            }
        }

        items = List.copyOf(loaded);
        Sej.LOGGER.info("Loaded {} entries into the creative item list", items.size());
    }

    private static void readSection(String name, JsonArray array, List<ItemStack> out) {
        for (JsonElement element : array) {
            String id = element.getAsString();
            Identifier identifier = Identifier.tryParse(id);
            Item item = identifier == null ? Items.AIR : Registries.ITEM.get(identifier);
            if (item == Items.AIR) {
                Sej.LOGGER.warn("Item list section '{}': unknown item '{}', skipped", name, id);
                continue;
            }
            out.add(new ItemStack(item));
        }
    }
}
