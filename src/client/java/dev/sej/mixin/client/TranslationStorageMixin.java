package dev.sej.mixin.client;

import dev.sej.ItemNames;
import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Map;

@Mixin(TranslationStorage.class)
public abstract class TranslationStorageMixin {

    @ModifyArg(
            method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resource/language/TranslationStorage;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resource/language/TranslationStorage;<init>(Ljava/util/Map;Z)V"),
            index = 0
    )
    private static Map<String, String> sej$applyItemNames(Map<String, String> translations) {
        return ItemNames.apply(translations);
    }
}