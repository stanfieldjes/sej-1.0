package dev.sej.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class SpiderWebAttackMixin {

    @Unique private static final float WEB_CHANCE = 1.0f;

    @Unique private static final float UPPER_WEB_CHANCE = 0.5f;

    @Inject(method = "damage", at = @At("RETURN"))
    private void bettabeta$webOnSpiderHit(DamageSource source, float amount,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (!(source.getAttacker() instanceof SpiderEntity)) {
            return;
        }

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.isCreative()) {
            return;
        }

        World world = player.getWorld();
        if (world.isClient()) {
            return;
        }

        BlockPos feet = player.getBlockPos();
        BlockPos head = feet.up();

        if (!bettabeta$roll(world, WEB_CHANCE)) {
            return;
        }

        if (bettabeta$placeWeb(world, feet)) {
            return;
        }

        if (bettabeta$roll(world, UPPER_WEB_CHANCE)) {
            bettabeta$placeWeb(world, head);
        }
    }

    @Unique
    private static boolean bettabeta$roll(World world, float chance) {
        if (chance <= 0.0f) {
            return false;
        }
        return chance >= 1.0f || world.getRandom().nextFloat() < chance;
    }

    @Unique
    private static boolean bettabeta$placeWeb(World world, BlockPos pos) {
        if (!world.getBlockState(pos).isAir()) {
            return false;
        }
        world.setBlockState(pos, Blocks.COBWEB.getDefaultState());
        return true;
    }
}