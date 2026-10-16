package dev.sej.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique private static final int SEJ_FULL_WIDTH = 204;
    @Unique private static final int SEJ_HALF_WIDTH = 100;
    @Unique private static final int SEJ_HEIGHT = 20;
    @Unique private static final int SEJ_SPACING = 24;

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sej$rebuildTitle(CallbackInfo ci) {
        this.clearChildren();
        this.setFocused(null);

        MinecraftClient client = MinecraftClient.getInstance();

        int left = this.width / 2 - SEJ_FULL_WIDTH / 2;
        int right = left + SEJ_FULL_WIDTH - SEJ_HALF_WIDTH;
        int y = this.height / 4 + 48;

        // Singleplayer
        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("sej.menu.singleplayer"),
                        b -> client.setScreen(new SelectWorldScreen(this)))
                .dimensions(left, y, SEJ_FULL_WIDTH, SEJ_HEIGHT)
                .build());
        y += SEJ_SPACING;

        // Multiplayer
        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("sej.menu.multiplayer"), b -> {
                    Screen next = client.options.skipMultiplayerWarning
                            ? new MultiplayerScreen(this)
                            : new MultiplayerWarningScreen(this);
                    client.setScreen(next);
                })
                .dimensions(left, y, SEJ_FULL_WIDTH, SEJ_HEIGHT)
                .build());
        y += SEJ_SPACING + 8;

        // Options (left) / Quit (right)
        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("sej.menu.options"),
                        b -> client.setScreen(new OptionsScreen(this, client.options)))
                .dimensions(left, y, SEJ_HALF_WIDTH, SEJ_HEIGHT)
                .build());

        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("sej.menu.quit"), b -> client.scheduleStop())
                .dimensions(right, y, SEJ_HALF_WIDTH, SEJ_HEIGHT)
                .build());
    }
}