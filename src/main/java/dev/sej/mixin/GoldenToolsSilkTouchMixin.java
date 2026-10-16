package dev.sej.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class GoldenToolsSilkTouchMixin {

    @Inject(method = "afterBreak", at = @At("HEAD"), cancellable = true)
    private void goldToolsDropBlock(
            World world,
            PlayerEntity player,
            BlockPos pos,
            BlockState state,
            net.minecraft.block.entity.BlockEntity blockEntity,
            ItemStack stack,
            CallbackInfo ci
    ) {

        if (!world.isClient && isGoldenTool(stack) && !(state.getBlock() instanceof SpawnerBlock)) {
            Block.dropStack(world, pos, new ItemStack(state.getBlock()));
            ci.cancel();
        }
    }

    @Unique
    private boolean isGoldenTool(ItemStack stack) {
        return stack.isOf(Items.GOLDEN_PICKAXE)
                || stack.isOf(Items.GOLDEN_AXE)
                || stack.isOf(Items.GOLDEN_SHOVEL)
                || stack.isOf(Items.GOLDEN_SWORD)
                || stack.isOf(Items.GOLDEN_HOE);
    }
}