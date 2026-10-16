package dev.sej.mixin;

import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSkeletonEntity.class)
public abstract class SkeletonEntityMixin {

    @Unique private static final float ARROW_SPEED = 1.0F;

    // Vanilla arrow physics, applied once per tick after the arrow moves.
    @Unique private static final double ARROW_DRAG = 0.99;
    @Unique private static final double ARROW_GRAVITY = 0.05;

    @Unique private static final double MIN_PITCH = Math.toRadians(-80);
    @Unique private static final double MAX_PITCH = Math.toRadians(45);

    @Inject(method = "createAbstractSkeletonAttributes", at = @At("RETURN"))
    private static void bettaBetaReduceSkeletonHealth(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        DefaultAttributeContainer.Builder attributes = cir.getReturnValue();
        attributes.add(EntityAttributes.GENERIC_MAX_HEALTH, 15.0);
    }

    /** Skeletons fire slower arrows. */
    @ModifyArg(method = "attack", index = 3, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;setVelocity(DDDFF)V"))
    private float bettabeta$slowerArrows(float speed) {
        return ARROW_SPEED;
    }

    /**
     * Replaces vanilla's flat "aim 0.2 higher per block of distance" with a real arc:
     * finds the angle that lands the arrow on the target at its actual distance and height.
     */
    @ModifyArg(method = "attack", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/projectile/PersistentProjectileEntity;setVelocity(DDDFF)V"))
    private double bettabeta$aimForDistance(double x, double y, double z, float speed, float divergence) {
        double distance = Math.sqrt(x * x + z * z);
        if (distance < 1.0E-4) return y;

        // Undo vanilla's guess to get the real height of the target above the arrow.
        double rise = y - distance * (double) 0.2F;

        // setVelocity only uses the direction, so return the height that gives this pitch.
        return distance * Math.tan(bettabeta$solvePitch(distance, rise));
    }

    /** Lowest angle that hits (distance, rise). Height at the target rises with pitch, so binary search it. */
    @Unique
    private static double bettabeta$solvePitch(double distance, double rise) {
        double low = MIN_PITCH;
        double high = MAX_PITCH;

        // Out of reach even at 45 degrees: lob it as far as it will go.
        if (bettabeta$heightAt(high, distance) < rise) return high;

        for (int i = 0; i < 30; i++) {
            double mid = (low + high) / 2;
            if (bettabeta$heightAt(mid, distance) < rise) low = mid;
            else high = mid;
        }
        return (low + high) / 2;
    }

    /** Steps the arrow tick by tick and returns its height when it has travelled `distance` sideways. */
    @Unique
    private static double bettabeta$heightAt(double pitch, double distance) {
        double vh = ARROW_SPEED * Math.cos(pitch);
        double vy = ARROW_SPEED * Math.sin(pitch);
        double h = 0;
        double y = 0;

        for (int tick = 0; tick < 200; tick++) {
            double nextH = h + vh;
            double nextY = y + vy;
            if (nextH >= distance) {
                double t = (distance - h) / (nextH - h);
                return y + (nextY - y) * t;
            }
            h = nextH;
            y = nextY;
            vh *= ARROW_DRAG;
            vy = vy * ARROW_DRAG - ARROW_GRAVITY;
        }
        return Double.NEGATIVE_INFINITY;
    }
}