package dev.sej.mixin.client;

import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(method = "processF3", at = @At("HEAD"), cancellable = true)
    private void sej$blockF3Combos(int key, CallbackInfoReturnable<Boolean> cir) {
        if (key != GLFW.GLFW_KEY_F3) {
            cir.setReturnValue(true);
        }
    }
}