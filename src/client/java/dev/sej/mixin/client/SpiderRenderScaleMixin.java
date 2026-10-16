package dev.sej.mixin.client;

import dev.sej.SpiderTweaks;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SpiderEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class SpiderRenderScaleMixin {

    @Inject(method = "scale", at = @At("TAIL"))
    private void sej$scaleSpiderModel(LivingEntity entity, MatrixStack matrices, float amount, CallbackInfo ci) {
        if (entity instanceof SpiderEntity) {
            float s = SpiderTweaks.SIZE_SCALE;
            matrices.scale(s, s, s);
        }
    }
}