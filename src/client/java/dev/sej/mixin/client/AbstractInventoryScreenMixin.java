package dev.sej.mixin.client;

import dev.sej.ItemListPanel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Status effects draw in the same spot as the item list, so hide them while it's open. */
@Mixin(AbstractInventoryScreen.class)
public abstract class AbstractInventoryScreenMixin {

    // require = 0: purely cosmetic, never worth a crash if the method moves.
    @Inject(method = "drawStatusEffects", at = @At("HEAD"), cancellable = true, require = 0)
    private void sej$hideEffectsUnderItemList(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if ((Object) this instanceof InventoryScreen && ItemListPanel.isShowing()) {
            ci.cancel();
        }
    }
}
