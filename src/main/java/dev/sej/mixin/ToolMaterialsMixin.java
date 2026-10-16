package dev.sej.mixin;

import net.minecraft.item.ToolMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ToolMaterials.class)
public class ToolMaterialsMixin {

    @Inject(method = "getMiningLevel", at = @At("HEAD"), cancellable = true)
    private void overrideMiningLevel(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this == ToolMaterials.WOOD) cir.setReturnValue(0);
        else if ((Object) this == ToolMaterials.STONE) cir.setReturnValue(1);
        else if ((Object) this == ToolMaterials.GOLD) cir.setReturnValue(2);
        else if ((Object) this == ToolMaterials.IRON) cir.setReturnValue(2);
        else if ((Object) this == ToolMaterials.DIAMOND) cir.setReturnValue(3);
    }
}