package dev.sej.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import dev.sej.ScoreText;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.screen.GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    @Unique private static final int SEJ_BUTTON_WIDTH = 204;
    @Unique private static final int SEJ_HALF_WIDTH = 100;
    @Unique private static final int SEJ_HEIGHT = 20;
    @Unique private static final int SEJ_SPACING = 24;

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void sej$rebuildMenu(CallbackInfo ci) {
        this.clearChildren();
        this.setFocused(null);

        MinecraftClient client = MinecraftClient.getInstance();

        int left = this.width / 2 - SEJ_BUTTON_WIDTH / 2;
        int y = this.height / 4 + 40;

        // Back to Game
        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("sej.menu.back_to_game"), b -> {
                    client.setScreen(null);
                    client.mouse.lockCursor();
                })
                .dimensions(left, y, SEJ_BUTTON_WIDTH, SEJ_HEIGHT)
                .build());
        y += SEJ_SPACING;

        // Options (left) / Open to LAN (right)
        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("sej.menu.options"),
                        b -> client.setScreen(new OptionsScreen(this, client.options)))
                .dimensions(left, y, SEJ_HALF_WIDTH, SEJ_HEIGHT)
                .build());

        ButtonWidget lanButton = ButtonWidget
                .builder(Text.translatable("sej.menu.open_to_lan"),
                        b -> client.setScreen(new OpenToLanScreen(this)))
                .dimensions(left + SEJ_BUTTON_WIDTH - SEJ_HALF_WIDTH, y,
                        SEJ_HALF_WIDTH, SEJ_HEIGHT)
                .build();
        // Only meaningful on a local single-player world that isn't already published.
        lanButton.active = client.isIntegratedServerRunning()
                && client.getServer() != null
                && !client.getServer().isRemote();
        this.addDrawableChild(lanButton);
        y += SEJ_SPACING;

        // Save and Quit to Title
        boolean singlePlayer = client.isInSingleplayer();
        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable(singlePlayer
                        ? "menu.returnToMenu"
                        : "menu.disconnect"), b -> {
                    b.active = false;
                    if (client.world != null) {
                        client.world.disconnect();
                    }
                    client.disconnect(new MessageScreen(Text.translatable("menu.savingLevel")));
                    client.setScreen(new TitleScreen());
                })
                .dimensions(left, y, SEJ_BUTTON_WIDTH, SEJ_HEIGHT)
                .build());
    }

    @Unique
    private static boolean sej$isCreative(MinecraftClient client) {
        return client.interactionManager != null
                && client.interactionManager.getCurrentGameMode() == GameMode.CREATIVE;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void sej$renderScore(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || sej$isCreative(client)) {
            return;
        }

        context.drawCenteredTextWithShadow(this.textRenderer, ScoreText.of(player),
                this.width / 2, this.height / 4 + 20, 0xFFFFFF);
    }
}