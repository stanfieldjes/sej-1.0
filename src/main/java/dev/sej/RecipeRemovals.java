package dev.sej;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RecipeRemovals {
    private static final Logger LOGGER = LoggerFactory.getLogger("sej");
    private static final Identifier FILE = new Identifier("sej", "recipe_removals.json");

    private RecipeRemovals() {}

    public static void apply(Map<Identifier, JsonElement> recipes, ResourceManager resources) {
        Optional<Resource> resource = resources.getResource(FILE);
        if (resource.isEmpty()) {
            LOGGER.warn("{} not found, no recipes removed", FILE);
            return;
        }

        JsonObject config;
        try (Reader reader = resource.get().getReader()) {
            config = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("Couldn't read {}", FILE, e);
            return;
        }

        Set<String> ids = strings(config, "ids");
        Set<String> inputs = strings(config, "inputs");
        Set<String> outputs = strings(config, "outputs");
        int before = recipes.size();

        recipes.entrySet().removeIf(entry -> {
            Identifier id = entry.getKey();
            if (ids.contains(id.toString())) return true;
            if (id.getNamespace().equals("sej")) return false; // the pack's own recipes stay
            return usesAny(entry.getValue(), inputs) || makesAny(entry.getValue(), outputs);
        });

        LOGGER.info("Removed {} recipes", before - recipes.size());
    }

    private static Set<String> strings(JsonObject config, String key) {
        Set<String> out = new HashSet<>();
        if (config.get(key) instanceof JsonArray array) {
            for (JsonElement element : array) out.add(element.getAsString());
        }
        return out;
    }

    /** True if any ingredient anywhere in the recipe (but not its result) is one of these items. */
    private static boolean usesAny(JsonElement json, Set<String> items) {
        if (items.isEmpty()) return false;
        if (json instanceof JsonArray array) {
            for (JsonElement element : array) if (usesAny(element, items)) return true;
        } else if (json instanceof JsonObject object) {
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if (key.equals("result")) continue;
                if (key.equals("item") && value.isJsonPrimitive() && items.contains(value.getAsString())) return true;
                if (usesAny(value, items)) return true;
            }
        }
        return false;
    }

    /** True if the recipe's result is one of these items. */
    private static boolean makesAny(JsonElement json, Set<String> items) {
        if (items.isEmpty() || !(json instanceof JsonObject object)) return false;
        JsonElement result = object.get("result");
        if (result == null) return false;
        if (result.isJsonPrimitive()) return items.contains(result.getAsString());
        if (result instanceof JsonObject resultObject && resultObject.get("item") != null) {
            return items.contains(resultObject.get("item").getAsString());
        }
        return false;
    }
}