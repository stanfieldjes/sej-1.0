package dev.sej.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class SleepingChatScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void cancelSleepScreen(net.minecraft.client.gui.screen.Screen screen, CallbackInfo ci) {
        if (screen instanceof SleepingChatScreen) {
            ci.cancel();
        }
    }
}