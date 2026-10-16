package dev.sej.mixin;

import dev.sej.GunpowderTrailBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets gunpowder be placed on the ground like redstone. Hooking Item#useOnBlock (rather than a
 * use-block event) means chests, doors etc. still open when right-clicked while holding gunpowder.
 */
@Mixin(Item.class)
public abstract class GunpowderPlaceMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void bettabeta$placeGunpowder(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (context.getStack().isOf(Items.GUNPOWDER)) {
            cir.setReturnValue(GunpowderTrailBlock.place(context));
        }
    }
}