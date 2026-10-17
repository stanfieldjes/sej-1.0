package dev.sej.mixin;

import com.google.gson.JsonElement;
import dev.sej.RecipeRemovals;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V",
            at = @At("HEAD"))
    private void sej$removeRecipes(Map<Identifier, JsonElement> recipes, ResourceManager resources, Profiler profiler,
                                   CallbackInfo ci) {
        RecipeRemovals.apply(recipes, resources);
    }
}