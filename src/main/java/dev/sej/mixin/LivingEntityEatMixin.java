package dev.sej.mixin;

import dev.sej.FoodHealConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityEatMixin {

    /** Food heals its full amount the moment it's eaten. Health syncs to the client on its own. */
    @Inject(method = "eatFood", at = @At("HEAD"))
    private void onEatFood(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;

        Float health = FoodHealConfig.healthOf(stack.getItem());
        if (health != null) {
            ((LivingEntity) (Object) this).heal(health);
        }
    }
}