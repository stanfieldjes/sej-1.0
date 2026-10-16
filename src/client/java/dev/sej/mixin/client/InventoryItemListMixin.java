package dev.sej.mixin.client;

import dev.sej.ItemListPanel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Creative mode uses the normal survival inventory, plus an item list panel
 * that opens from a chest button in the top right of the inventory.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryItemListMixin extends AbstractInventoryScreen<PlayerScreenHandler> {

    @Unique private ItemListPanel sej$itemList;

    private InventoryItemListMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    /** Vanilla swaps to CreativeInventoryScreen from init() and handledScreenTick(); never do that. */
    @Redirect(method = {"init", "handledScreenTick"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;hasCreativeInventory()Z"))
    private boolean sej$keepSurvivalInventory(ClientPlayerInteractionManager manager) {
        return false;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sej$addItemList(CallbackInfo ci) {
        sej$itemList = new ItemListPanel(this.handler);
        sej$layout();
    }

    @Unique
    private void sej$layout() {
        if (sej$itemList == null) return;
        // Same rule vanilla uses: on narrow screens an open recipe book replaces the inventory.
        boolean inventoryShown = !(((InventoryScreen) (Object) this).getRecipeBookWidget().isOpen() && this.width < 379);
        sej$itemList.layout(this.x, this.y, this.backgroundWidth, this.backgroundHeight, this.width, inventoryShown);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void sej$layoutItemList(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        sej$layout();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void sej$renderItemList(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (sej$itemList != null) sej$itemList.render(context, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void sej$clickItemList(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (sej$itemList == null) return;
        sej$layout();
        if (sej$itemList.mouseClicked(mouseX, mouseY, button)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void sej$releaseItemList(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (sej$itemList != null && sej$itemList.mouseReleased()) cir.setReturnValue(true);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (sej$itemList != null && sej$itemList.mouseScrolled(mouseX, mouseY, amount)) return true;
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Inject(method = "handledScreenTick", at = @At("TAIL"))
    private void sej$tickItemList(CallbackInfo ci) {
        ItemListPanel.tick();
    }
}