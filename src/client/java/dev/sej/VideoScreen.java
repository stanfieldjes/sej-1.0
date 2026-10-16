package dev.sej;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Beta-style video settings: four rows of paired option widgets. Each widget
 * carries its own label, so nothing is drawn outside the columns.
 */
public class VideoScreen extends Screen {

    private static final int COLUMN_WIDTH = 150;
    private static final int WIDGET_HEIGHT = 20;
    private static final int ROW_SPACING = 24;
    private static final int CENTER_GAP = 5;

    private final Screen parent;

    private boolean receivedPress;

    public VideoScreen(Screen parent) {
        super(Text.translatable("options.videoTitle"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MinecraftClient client = MinecraftClient.getInstance();
        GameOptions options = client.options;

        int center = this.width / 2;
        int leftX = center - CENTER_GAP - COLUMN_WIDTH;
        int rightX = center + CENTER_GAP;
        int y = this.height / 2 - (4 * ROW_SPACING) / 2 - 8;

        // Row 1: FOV / Brightness
        add(options.getFov(), options, leftX, y);
        add(options.getGamma(), options, rightX, y);
        y += ROW_SPACING;

        // Row 2: GUI Scale / Graphics Quality
        this.addDrawableChild(options.getGuiScale().createWidget(options, leftX, y, COLUMN_WIDTH,
                value -> client.onResolutionChanged()));
        add(options.getGraphicsMode(), options, rightX, y);
        y += ROW_SPACING;

        // Row 3: View Bobbing / Particles
        add(options.getBobView(), options, leftX, y);
        add(options.getParticles(), options, rightX, y);
        y += ROW_SPACING;

        // Row 4: Fullscreen / Smooth Lighting
        // GUI scale needs an explicit resolution refresh; fullscreen applies itself
        // through the callback GameOptions already attaches to that option.
        add(options.getFullscreen(), options, leftX, y);
        add(options.getAo(), options, rightX, y);

        this.addDrawableChild(ButtonWidget
                .builder(ScreenTexts.DONE, b -> this.close())
                .dimensions(center - 100, this.height - 27, 200, WIDGET_HEIGHT)
                .build());
    }

    private <T> void add(SimpleOption<T> option, GameOptions options, int x, int y) {
        ClickableWidget widget = option.createWidget(options, x, y, COLUMN_WIDTH);
        this.addDrawableChild(widget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Match whichever renderBackground overload you used in BetaKeybindsScreen.
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void removed() {
        MinecraftClient.getInstance().options.write();
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.receivedPress = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.receivedPress) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}