package dev.sej.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.OpenToLanScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OpenToLanScreen.class)
public abstract class OpenToLanScreenMixin extends Screen {

    @Shadow @Final private Screen parent;

    @Unique private static final int SEJ_WIDTH = 204;
    @Unique private static final int SEJ_HEIGHT = 20;

    @Unique private TextFieldWidget sej$portField;
    @Unique private ButtonWidget sej$startButton;

    protected OpenToLanScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sej$rebuild(CallbackInfo ci) {
        this.clearChildren();
        this.setFocused(null);

        MinecraftClient client = MinecraftClient.getInstance();

        int left = this.width / 2 - SEJ_WIDTH / 2;
        int fieldY = this.height / 4 + 40;

        this.sej$portField = new TextFieldWidget(
                this.textRenderer, left, fieldY, SEJ_WIDTH, SEJ_HEIGHT,
                Text.translatable("sej.lan.port"));
        this.sej$portField.setMaxLength(5);
        this.sej$portField.setText(String.valueOf(sej$findFreePort()));
        this.sej$portField.setChangedListener(text -> this.sej$updateStartButton());
        this.addDrawableChild(this.sej$portField);

        this.sej$startButton = ButtonWidget
                .builder(Text.translatable("sej.lan.start"), b -> {
                    Integer port = this.sej$parsePort();
                    if (port == null) {
                        return;
                    }
                    if (client.getServer() != null && client.interactionManager != null) {
                        boolean published = client.getServer().openToLan(
                                client.interactionManager.getCurrentGameMode(),
                                client.getServer().getPlayerManager().areCheatsAllowed(),
                                port);

                        Text message = published
                                ? Text.translatable("commands.publish.started", port)
                                : Text.translatable("commands.publish.failed");
                        client.inGameHud.getChatHud().addMessage(message);
                        client.updateWindowTitle();
                    }
                    client.setScreen(null);
                })
                .dimensions(left, fieldY + 40, SEJ_WIDTH, SEJ_HEIGHT)
                .build();
        this.addDrawableChild(this.sej$startButton);

        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("sej.lan.back"), b -> client.setScreen(this.parent))
                .dimensions(left, fieldY + 64, SEJ_WIDTH, SEJ_HEIGHT)
                .build());

        this.setInitialFocus(this.sej$portField);
        this.sej$updateStartButton();
    }

    @Unique
    private Integer sej$parsePort() {
        try {
            int port = Integer.parseInt(this.sej$portField.getText().trim());
            return (port >= 1024 && port <= 65535) ? port : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Unique
    private static int sej$findFreePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            return 25565;
        }
    }

    @Unique
    private void sej$updateStartButton() {
        if (this.sej$startButton != null) {
            this.sej$startButton.active = this.sej$parsePort() != null;
        }
    }

    /** Replaces vanilla's render, which draws its own title, info line and port label. */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void sej$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int left = this.width / 2 - SEJ_WIDTH / 2;
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("sej.lan.port"), left, this.height / 4 + 28, 0xA0A0A0);

        if (this.sej$parsePort() == null) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Port must be 1024–65535").formatted(Formatting.RED),
                    this.width / 2, this.height / 4 + 66, 0xFFFFFF);
        }

        ci.cancel();
    }
}