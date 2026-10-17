package dev.sej;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Small icon button (two people) that sits at the right end of the chat input bar.
 * Mouse only: arrow keys and Tab never move focus onto it.
 */
public class SocialButton extends ClickableWidget {
    private static final Identifier ICON = new Identifier("sej", "textures/gui/social.png");
    private static final int ICON_WIDTH = 11;
    private static final int ICON_HEIGHT = 9;

    /** Width and height of the clickable area (same height as the chat input bar). */
    public static final int SIZE = 12;

    private final Runnable action;

    public SocialButton(int x, int y, Text label, Runnable action) {
        super(x, y, SIZE, SIZE, label);
        this.action = action;
        this.setTooltip(Tooltip.of(label));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.action.run();
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        if (hovered) {
            context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x30FFFFFF);
        }

        // Dimmed until hovered, so it doesn't pull attention from the chat.
        float shade = hovered ? 1.0F : 0.7F;
        context.setShaderColor(shade, shade, shade, 1.0F);
        RenderSystem.enableBlend();
        context.drawTexture(ICON,
                this.getX() + (this.width - ICON_WIDTH) / 2,
                this.getY() + (this.height - ICON_HEIGHT) / 2,
                0, 0, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);
        RenderSystem.disableBlend();
        context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Keeps arrow keys and Tab from ever landing here, so Up still scrolls chat history. */
    @Override
    public @Nullable GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
        return null;
    }

    /** Never holds focus, so typing always stays in the chat box. */
    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}