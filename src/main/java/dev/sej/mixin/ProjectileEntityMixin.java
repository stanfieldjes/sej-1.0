package dev.sej.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {
    @ModifyVariable(method = "setVelocity(DDDFF)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float removePlayerOnlySpread(float divergence) {
        ProjectileEntity self = (ProjectileEntity)(Object)this;
        return (self.getOwner() instanceof PlayerEntity) ? 0.0f : divergence;
    }
}