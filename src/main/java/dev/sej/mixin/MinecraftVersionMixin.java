package dev.sej.mixin;

import dev.sej.Sej;
import net.minecraft.MinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftVersion.class)
public class MinecraftVersionMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void changeName(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("SEJ " + Sej.VERSION);
    }
}