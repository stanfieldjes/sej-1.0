package dev.sej.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceScreenHandler.class)
public abstract class FurnaceShiftClickMixin extends ScreenHandler {

    protected FurnaceShiftClickMixin(ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void handleCustomPorkchopShiftClick(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        Slot clickedSlot = this.slots.get(index);

        if (!clickedSlot.hasStack()) return;
        ItemStack clickedStack = clickedSlot.getStack();

        if (index == 2 && clickedStack.isOf(Items.COOKED_PORKCHOP)) {
            ItemStack singleItem = clickedStack.copy();
            singleItem.setCount(1);

            if (this.insertItem(singleItem, 3, 39, true)) {
                clickedStack.decrement(1);
                clickedSlot.onTakeItem(player, singleItem);
                clickedSlot.markDirty();
            }
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        if (index == 0 && clickedStack.isOf(Items.PORKCHOP)) {
            ItemStack singleItem = clickedStack.copy();
            singleItem.setCount(1);

            if (this.insertItem(singleItem, 3, 39, true)) {
                clickedStack.decrement(1);
                clickedSlot.markDirty();
            }
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        if (index >= 3 && clickedStack.isOf(Items.PORKCHOP)) {
            Slot inputSlot = this.slots.get(0);
            ItemStack inputStack = inputSlot.getStack();
            int maxAllowed = 8;

            if (inputStack.isEmpty()) {
                int moveAmount = Math.min(clickedStack.getCount(), maxAllowed);
                inputSlot.setStack(clickedStack.split(moveAmount));
                inputSlot.markDirty();
                cir.setReturnValue(ItemStack.EMPTY);

            } else if (inputStack.isOf(Items.PORKCHOP) && inputStack.getCount() < maxAllowed) {
                int spaceLeft = maxAllowed - inputStack.getCount();
                int moveAmount = Math.min(clickedStack.getCount(), spaceLeft);

                inputStack.increment(moveAmount);
                clickedStack.decrement(moveAmount);
                inputSlot.markDirty();
                cir.setReturnValue(ItemStack.EMPTY);
            }
        }
    }
}