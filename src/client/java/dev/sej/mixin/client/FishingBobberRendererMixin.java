package dev.sej.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import dev.sej.SlimedBobber;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingBobberEntityRenderer.class)
public abstract class FishingBobberRendererMixin {

    @Unique
    private static final RenderLayer SEJ_SLIME_LAYER =
            RenderLayer.getEntityCutout(new Identifier("sej", "textures/entity/fishing_hook_slime.png"));

    @Shadow
    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, int light,
                               float x, int y, int u, int v) {
        throw new AssertionError(); // replaced by the real method at runtime
    }

    @Inject(
            method = "render(Lnet/minecraft/entity/projectile/FishingBobberEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 0))
    private void sej$slimeOnHook(CallbackInfo ci,
                                 @Local(argsOnly = true) FishingBobberEntity bobber,
                                 @Local(argsOnly = true) MatrixStack matrices,
                                 @Local(argsOnly = true) VertexConsumerProvider consumers,
                                 @Local(argsOnly = true) int light) {
        if (!((SlimedBobber) bobber).sej$isSlimed()) return;

        MatrixStack.Entry entry = matrices.peek();
        VertexConsumer buffer = consumers.getBuffer(SEJ_SLIME_LAYER);
        vertex(buffer, entry.getPositionMatrix(), entry.getNormalMatrix(), light, 0.0F, 0, 0, 1);
        vertex(buffer, entry.getPositionMatrix(), entry.getNormalMatrix(), light, 1.0F, 0, 1, 1);
        vertex(buffer, entry.getPositionMatrix(), entry.getNormalMatrix(), light, 1.0F, 1, 1, 0);
        vertex(buffer, entry.getPositionMatrix(), entry.getNormalMatrix(), light, 0.0F, 1, 0, 0);
    }
}