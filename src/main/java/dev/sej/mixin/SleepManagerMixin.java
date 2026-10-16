package dev.sej.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.SleepManager;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SleepManager.class)
public class SleepManagerMixin {

    @Inject(method = "canResetTime", at = @At("HEAD"), cancellable = true)
    private void cancelNightSkip(int percentage,
                                             List<ServerPlayerEntity> players, CallbackInfoReturnable<Boolean> cir) {
        if (players.isEmpty()) return;
        cir.setReturnValue(false);
    }
}