package dev.sej.mixin;

import dev.sej.FoodHealConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemFoodMixin {

    /** Can't eat at full health. */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void preventOvereating(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack stack = user.getStackInHand(hand);

        if (FoodHealConfig.isFood(stack.getItem()) && user.getHealth() >= user.getMaxHealth()) {
            cir.setReturnValue(TypedActionResult.fail(stack));
        }
    }
}