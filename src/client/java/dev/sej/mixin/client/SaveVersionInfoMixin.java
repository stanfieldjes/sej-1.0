package dev.sej.mixin.client;

import dev.sej.Sej;
import net.minecraft.world.level.storage.SaveVersionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SaveVersionInfo.class)
public class SaveVersionInfoMixin {

    @Inject(method = "getVersionName", at = @At("RETURN"), cancellable = true)
    private void changeVersionName(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("SEJ " + Sej.VERSION);
    }
}