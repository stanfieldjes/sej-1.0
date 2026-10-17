package dev.sej.mixin;

import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class GoldenAppleMixin {
    @Unique private static FoodComponent sej$plainGoldenApple;

    @Inject(method = "getFoodComponent", at = @At("RETURN"), cancellable = true)
    private void sej$noGoldenAppleEffects(CallbackInfoReturnable<FoodComponent> cir) {
        if ((Object) this != Items.GOLDEN_APPLE) return;
        FoodComponent original = cir.getReturnValue();
        if (original == null) return;
        if (sej$plainGoldenApple == null) {
            sej$plainGoldenApple = new FoodComponent.Builder()
                    .hunger(original.getHunger())
                    .saturationModifier(original.getSaturationModifier())
                    .build();
        }
        cir.setReturnValue(sej$plainGoldenApple);
    }
}