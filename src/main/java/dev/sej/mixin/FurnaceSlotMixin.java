package dev.sej.mixin;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class FurnaceSlotMixin {

    @Shadow @Final public Inventory inventory;

    @Inject(method = "getMaxItemCount(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void allowPorkchopStacks(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (this.inventory instanceof AbstractFurnaceBlockEntity) {
            if (stack.isOf(Items.PORKCHOP) || stack.isOf(Items.COOKED_PORKCHOP)) {
                cir.setReturnValue(8);
            }
        }
    }

    @ModifyVariable(method = "takeStack", at = @At("HEAD"), argsOnly = true)
    private int limitManualTakeAmount(int amount) {
        Slot slot = (Slot) (Object) this;

        if (slot.hasStack()) {
            ItemStack stack = slot.getStack();

            if (slot instanceof FurnaceOutputSlot && stack.isOf(Items.COOKED_PORKCHOP)) {
                return 1;
            }

            if (this.inventory instanceof AbstractFurnaceBlockEntity && stack.isOf(Items.PORKCHOP)) {
                if (amount < stack.getCount()) {
                    return 1;
                }
            }
        }

        return amount;
    }
}