package dev.sej.mixin.client;

import dev.sej.EquipmentData;
import dev.sej.FoodHealConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @ModifyVariable(method = "getTooltip", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private TooltipContext sej$forceBasic(TooltipContext context) {
        return TooltipContext.Default.BASIC;
    }

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void sej$customTooltip(CallbackInfoReturnable<List<Text>> cir) {
        List<Text> lines = cir.getReturnValue();
        if (lines == null) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) {
            return;
        }

        Item item = self.getItem();
        boolean equipment = EquipmentData.isTracked(item);
        boolean food = FoodHealConfig.isFood(item);

        if (!equipment && !food) {
            return;
        }

        lines.clear();
        lines.add(self.getName().copy());

        if (!Screen.hasShiftDown()) {
            return;
        }

        Integer max = EquipmentData.maxDamage(item);
        int maxDurability = max != null ? max : self.getMaxDamage();
        if (maxDurability > 0) {
            int remaining = maxDurability - self.getDamage();

            lines.add(Text.literal("Durability: ").formatted(Formatting.GRAY)
                    .append(Text.literal(remaining + "/" + maxDurability).formatted(Formatting.GRAY)));
        }
    }
}