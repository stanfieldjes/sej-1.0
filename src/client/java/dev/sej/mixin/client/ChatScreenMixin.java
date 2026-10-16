package dev.sej.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While chat is open, outlines the chat area and lets you drag its top edge (height),
 * right edge (width) or top-right corner (both). The sizes go into the normal
 * Chat Width / Chat Height options, so they save with the rest of the settings.
 *
 * Also adds a Groups button (bottom right, above the text box) that opens
 * Simple Voice Chat's group screen.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    /** How close (in GUI pixels) the mouse has to be to an edge to grab it. */
    @Unique private static final int GRAB = 3;
    /** Vanilla's chat background reaches this far past the text width (in chat-scaled pixels). */
    @Unique private static final int BACKGROUND_PAD = 12;

    @Unique private static final int COLOR_EDGE = 0x60FFFFFF;
    @Unique private static final int COLOR_EDGE_ACTIVE = 0xFFFFFFFF;

    // Vanilla option ranges: width 40-320 px, height 20-180 px.
    @Unique private static final double MIN_WIDTH = 40, WIDTH_RANGE = 280;
    @Unique private static final double MIN_HEIGHT = 20, HEIGHT_RANGE = 160;

    /** Simple Voice Chat's "Groups" key binding. */
    @Unique private static final String SVC_GROUP_KEY = "key.voice_chat_group";
    @Unique private static final int GROUPS_WIDTH = 60;
    @Unique private static final int GROUPS_HEIGHT = 20;

    @Unique private boolean sej$dragWidth;
    @Unique private boolean sej$dragHeight;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    /** Chat area on screen as {left, top, right, bottom}, matching where vanilla draws it. */
    @Unique
    private int[] sej$box() {
        MinecraftClient client = MinecraftClient.getInstance();
        ChatHud chat = client.inGameHud.getChatHud();
        double scale = chat.getChatScale();
        int lineHeight = (int) (9.0 * (client.options.getChatLineSpacing().getValue() + 1.0));

        int bottomLocal = MathHelper.floor((this.height - 40) / scale);
        int widthLocal = MathHelper.ceil(chat.getWidth() / scale);

        int bottom = (int) Math.round(bottomLocal * scale);
        int top = (int) Math.round((bottomLocal - chat.getVisibleLineCount() * lineHeight) * scale);
        int right = (int) Math.round((widthLocal + BACKGROUND_PAD) * scale);
        return new int[]{0, top, right, bottom};
    }

    @Unique
    private static boolean sej$onRight(int[] box, double x, double y) {
        return Math.abs(x - box[2]) <= GRAB && y >= box[1] - GRAB && y <= box[3];
    }

    @Unique
    private static boolean sej$onTop(int[] box, double x, double y) {
        return Math.abs(y - box[1]) <= GRAB && x >= box[0] && x <= box[2] + GRAB;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void sej$addGroupsButton(CallbackInfo ci) {
        KeyBinding groupKey = sej$findKey(SVC_GROUP_KEY);
        if (groupKey == null) return; // Simple Voice Chat not installed

        // Right-hand side, just above the chat text box.
        this.addDrawableChild(ButtonWidget
                .builder(Text.translatable("sej.chat.groups"), b -> {
                    // Close chat, then press Simple Voice Chat's Groups key for one tick.
                    // Its own handler then opens your group, the join screen, or says groups are off.
                    MinecraftClient.getInstance().setScreen(null);
                    KeyBindingAccessor key = (KeyBindingAccessor) groupKey;
                    key.sej$setTimesPressed(key.sej$getTimesPressed() + 1);
                })
                .dimensions(this.width - GROUPS_WIDTH - 2, this.height - 14 - 2 - GROUPS_HEIGHT, GROUPS_WIDTH, GROUPS_HEIGHT)
                .build());
    }

    @Unique
    private static KeyBinding sej$findKey(String translationKey) {
        for (KeyBinding binding : MinecraftClient.getInstance().options.allKeys) {
            if (binding.getTranslationKey().equals(translationKey)) return binding;
        }
        return null;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void sej$startResize(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        int[] box = this.sej$box();
        boolean width = sej$onRight(box, mouseX, mouseY);
        boolean height = sej$onTop(box, mouseX, mouseY);
        if (width || height) {
            this.sej$dragWidth = width;
            this.sej$dragHeight = height;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void sej$renderBounds(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) return;

        // Follow the mouse while the button is held; save once it's let go.
        if (this.sej$dragWidth || this.sej$dragHeight) {
            boolean held = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (held) {
                this.sej$applyDrag(client, mouseX, mouseY);
            } else {
                this.sej$dragWidth = false;
                this.sej$dragHeight = false;
                client.options.write();
            }
        }

        int[] box = this.sej$box();
        int left = box[0], top = box[1], right = box[2], bottom = box[3];

        boolean hoverRight = sej$onRight(box, mouseX, mouseY);
        boolean hoverTop = sej$onTop(box, mouseX, mouseY);
        int topColor = (this.sej$dragHeight || hoverTop) ? COLOR_EDGE_ACTIVE : COLOR_EDGE;
        int rightColor = (this.sej$dragWidth || hoverRight) ? COLOR_EDGE_ACTIVE : COLOR_EDGE;

        // Draw above the chat text and background.
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 200);
        context.fill(left, top, right, top + 1, topColor);
        context.fill(right - 1, top, right, bottom, rightColor);
        context.fill(left, top + 1, left + 1, bottom, COLOR_EDGE);
        context.fill(left + 1, bottom - 1, right - 1, bottom, COLOR_EDGE);
        // Corner grip, so it's clear the top-right drags both ways.
        boolean cornerActive = topColor == COLOR_EDGE_ACTIVE || rightColor == COLOR_EDGE_ACTIVE;
        context.fill(right - 4, top, right, top + 4, cornerActive ? COLOR_EDGE_ACTIVE : COLOR_EDGE);
        context.getMatrices().pop();
    }

    @Unique
    private void sej$applyDrag(MinecraftClient client, int mouseX, int mouseY) {
        GameOptions options = client.options;
        double scale = client.inGameHud.getChatHud().getChatScale();

        if (this.sej$dragWidth) {
            double widthPx = mouseX - BACKGROUND_PAD * scale;
            options.getChatWidth().setValue(MathHelper.clamp((widthPx - MIN_WIDTH) / WIDTH_RANGE, 0.0, 1.0));
        }

        if (this.sej$dragHeight) {
            int bottom = this.sej$box()[3];
            double heightPx = (bottom - mouseY) / scale;
            double value = MathHelper.clamp((heightPx - MIN_HEIGHT) / HEIGHT_RANGE, 0.0, 1.0);
            // Same height open or closed, so what you set here is what you see in game.
            options.getChatHeightFocused().setValue(value);
            options.getChatHeightUnfocused().setValue(value);
        }
    }
}