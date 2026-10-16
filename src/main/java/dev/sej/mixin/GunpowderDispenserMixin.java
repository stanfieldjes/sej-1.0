package dev.sej.mixin;

import dev.sej.GunpowderTrailBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Flint and steel in a dispenser lights a gunpowder trail directly in front of it.
 * Anything else (lighting fire, TNT, campfires...) falls through to the vanilla behavior.
 */
@Mixin(DispenserBlock.class)
public abstract class GunpowderDispenserMixin {
    @Inject(method = "getBehaviorForItem", at = @At("RETURN"), cancellable = true)
    private void bettabeta$lightGunpowder(ItemStack item, CallbackInfoReturnable<DispenserBehavior> cir) {
        if (!item.isOf(Items.FLINT_AND_STEEL)) return;
        DispenserBehavior vanilla = cir.getReturnValue();

        cir.setReturnValue((pointer, stack) -> {
            ServerWorld world = pointer.getWorld();
            Direction facing = pointer.getBlockState().get(DispenserBlock.FACING);
            BlockPos target = pointer.getPos().offset(facing);

            if (!GunpowderTrailBlock.ignite(world, target)) {
                return vanilla.dispense(pointer, stack);
            }

            world.playSound(null, target, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS,
                    1.0F, world.random.nextFloat() * 0.4F + 0.8F);
            world.syncWorldEvent(WorldEvents.DISPENSER_DISPENSES, pointer.getPos(), 0);
            world.syncWorldEvent(WorldEvents.DISPENSER_ACTIVATED, pointer.getPos(), facing.getId());
            if (stack.damage(1, world.random, null)) {
                stack.setCount(0);
            }
            return stack;
        });
    }
}