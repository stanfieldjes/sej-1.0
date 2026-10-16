package dev.sej.mixin.client;

import dev.sej.KeyBindsScreen;
import dev.sej.VideoScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import dev.sej.AudioScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    @Unique private static final int SEJ_FULL_WIDTH = 204;
    @Unique private static final int SEJ_HALF_WIDTH = 100;
    @Unique private static final int SEJ_HEIGHT = 20;
    @Unique private static final int SEJ_SPACING = 24;

    @Unique private Screen sej$parent;

    protected OptionsScreenMixin(Text title) {
        super(title);
    }

    /** Capture the parent screen without shadowing a private field. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void sej$captureParent(Screen parent, GameOptions options, CallbackInfo ci) {
        this.sej$parent = parent;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sej$rebuildOptions(CallbackInfo ci) {
        this.clearChildren();
        this.setFocused(null);

        MinecraftClient client = MinecraftClient.getInstance();
        GameOptions options = client.options;

        int left = this.width / 2 - SEJ_FULL_WIDTH / 2;
        int right = left + SEJ_FULL_WIDTH - SEJ_HALF_WIDTH;
        int y = this.height / 6 + 24;

        // Row 1: Main Volume (left) / Key Binds (right)
        ClickableWidget volumeSlider = options.getSoundVolumeOption(SoundCategory.MASTER)
                .createWidget(options, left, y, SEJ_HALF_WIDTH);
        this.addDrawableChild(volumeSlider);

        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("sej.options.controls"),
                        b -> client.setScreen(new KeyBindsScreen(this)))
                .dimensions(right, y, SEJ_HALF_WIDTH, SEJ_HEIGHT)
                .build());
        y += SEJ_SPACING;

        // Row 2: Video Settings (left) / Audio Settings (right)
        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("sej.options.video"),
                        b -> client.setScreen(new VideoScreen(this)))
                .dimensions(left, y, SEJ_HALF_WIDTH, SEJ_HEIGHT)
                .build());

        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("sej.options.audio"),
                        b -> client.setScreen(new AudioScreen(this)))
                .dimensions(right, y, SEJ_HALF_WIDTH, SEJ_HEIGHT)
                .build());
        y += SEJ_SPACING + 8;

        // Done
        this.addDrawableChild(ButtonWidget
                .builder(ScreenTexts.DONE, b -> {
                    options.write();
                    client.setScreen(this.sej$parent);
                })
                .dimensions(left, y, SEJ_FULL_WIDTH, SEJ_HEIGHT)
                .build());
    }
}