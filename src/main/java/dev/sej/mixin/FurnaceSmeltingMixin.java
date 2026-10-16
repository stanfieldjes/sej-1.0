package dev.sej.mixin;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractFurnaceBlockEntity.class)
public class FurnaceSmeltingMixin {

    @Redirect(method = "canAcceptRecipeOutput", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I"))
    private static int overrideSmeltingLimit(ItemStack stack) {
        if (stack.isOf(Items.COOKED_PORKCHOP)) {
            return 8;
        }
        return stack.getMaxCount();
    }
}