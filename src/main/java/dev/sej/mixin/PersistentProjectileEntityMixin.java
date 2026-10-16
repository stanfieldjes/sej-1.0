package dev.sej.mixin;

import dev.sej.ArrowDrawInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin {
    @Unique private float basePotentialDamage = -1f;
    @Unique private float intendedVelocity = -1f;

    @Unique
    private void setupArrowData() {
        PersistentProjectileEntity self = (PersistentProjectileEntity)(Object)this;
        if (self.getOwner() instanceof PlayerEntity && basePotentialDamage < 0) {
            this.basePotentialDamage = ArrowDrawInfo.lastPotentialDamage;
            this.intendedVelocity = ArrowDrawInfo.lastIntendedVelocity;
            self.setCritical(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void initOnTick(CallbackInfo ci) {
        setupArrowData();
    }

    @ModifyVariable(method = "onEntityHit", at = @At("STORE"), ordinal = 0)
    private int applyExactPlayerDamage(int originalDmg) {
        setupArrowData();
        PersistentProjectileEntity self = (PersistentProjectileEntity)(Object)this;

        if (!(self.getOwner() instanceof PlayerEntity) || basePotentialDamage < 0 || intendedVelocity <= 0) {
            return originalDmg;
        }

        double currentSpeed = self.getVelocity().length();
        float finalDmg = (float) (currentSpeed / intendedVelocity) * basePotentialDamage;
        return Math.max(1, Math.round(finalDmg));
    }

    @ModifyVariable(method = "tick", at = @At(value = "STORE"), ordinal = 0)
    private float removePlayerArrowDrag(float drag) {
        PersistentProjectileEntity self = (PersistentProjectileEntity)(Object)this;
        return (self.getOwner() instanceof PlayerEntity) ? 1.0f : drag;
    }
}