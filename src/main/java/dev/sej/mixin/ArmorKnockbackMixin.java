package dev.sej.mixin;

import dev.sej.ArmorCalculator;
import dev.sej.DamageCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(Explosion.class)
public class ArmorKnockbackMixin {

    @Inject(
            method = "collectBlocksAndDamageEntities()V",
            at = @At("RETURN")
    )
    private void scalePlayerBlastKnockback(CallbackInfo ci) {
        Explosion self = (Explosion) (Object) this;

        Map<PlayerEntity, Vec3d> affectedPlayers = self.getAffectedPlayers();

        for (PlayerEntity player : affectedPlayers.keySet()) {
            float multiplier = ArmorCalculator.computeDamageMultiplier(player, DamageCategory.BLAST);
            if (multiplier >= 1.0f) continue;

            affectedPlayers.computeIfPresent(player, (k, originalKnockback) -> originalKnockback.multiply(multiplier));
        }
    }
}