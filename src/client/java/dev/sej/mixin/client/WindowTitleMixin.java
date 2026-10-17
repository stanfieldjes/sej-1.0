package dev.sej.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
public abstract class WindowTitleMixin {

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void sej$windowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Sandbox: Endless Journey");
    }
}