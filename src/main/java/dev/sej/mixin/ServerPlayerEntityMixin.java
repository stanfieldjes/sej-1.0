package dev.sej.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Unique
    private int bb_sleepTicks = 0;

    /** Sleeping heals 1 health (half a heart) every second; sneaking gets you out of bed. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickSleepHeal(CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        if (!self.isSleeping()) {
            bb_sleepTicks = 0;
            return;
        }

        if (self.isSneaking()) {
            self.wakeUp();
            return;
        }

        bb_sleepTicks++;

        if (bb_sleepTicks % 20 == 0 && self.getHealth() < self.getMaxHealth()) {
            self.heal(1.0F);
        }
    }
}