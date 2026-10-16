package dev.sej.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MiningSpinUpMixin {

    @Unique private static final int FULL_SPEED_TICKS = 50;

    @Unique private int sej$spinUp;
    @Unique private boolean sej$minedThisTick;

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void sej$markMining(CallbackInfoReturnable<Boolean> cir) {
        this.sej$minedThisTick = true;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void sej$updateSpinUp(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean holdingBreak = client.options != null && client.options.attackKey.isPressed();

        if (!holdingBreak) {
            this.sej$spinUp = 0;
        } else if (this.sej$minedThisTick) {
            this.sej$spinUp = Math.min(FULL_SPEED_TICKS, this.sej$spinUp + 1);
        }
        this.sej$minedThisTick = false;
    }

    @ModifyConstant(method = "updateBlockBreakingProgress", constant = @Constant(intValue = 5))
    private int sej$rampCooldown(int vanillaCooldown) {
        int reduction = vanillaCooldown * this.sej$spinUp / FULL_SPEED_TICKS;
        return Math.max(0, vanillaCooldown - reduction);
    }
}