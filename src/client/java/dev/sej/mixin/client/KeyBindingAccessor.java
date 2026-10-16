package dev.sej.mixin.client;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets us "press" another mod's key binding, so its own handler runs exactly as if the key was hit. */
@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {

    @Accessor("timesPressed")
    int sej$getTimesPressed();

    @Accessor("timesPressed")
    void sej$setTimesPressed(int timesPressed);
}