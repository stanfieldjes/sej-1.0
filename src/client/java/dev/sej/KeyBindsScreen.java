package dev.sej;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class KeyBindsScreen extends Screen {

    private static final int BUTTON_WIDTH = 75;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_SPACING = 22;
    private static final int CENTER_GAP = 10;
    private static final int LABEL_GAP = 6;
    private static final int SLIDER_WIDTH = 150;
    private static final int SCROLLBAR_WIDTH = 6;

    private static final String ZOOMIFY_ZOOM = "zoomify.key.zoom";
    private static final String NT_TOGGLE_FOG = "key.nostalgic_tweaks.toggle_fog";
    private static final String SVC_PUSH_TO_TALK = "key.push_to_talk";
    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private KeyBinding selected;
    private boolean receivedPress;

    private int gridTop;
    private int gridBottom;
    private int contentTop;
    private int maxScroll;
    private int scroll;
    private boolean draggingScrollbar;

    private record Row(KeyBinding binding, ButtonWidget button, Text label,
                       boolean rightColumn, int index) {
    }

    public KeyBindsScreen(Screen parent) {
        super(Text.translatable("controls.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        GameOptions options = MinecraftClient.getInstance().options;
        this.rows.clear();

        List<KeyBinding> leftColumn = present(
                options.attackKey,
                options.useKey,
                options.forwardKey,
                options.backKey,
                options.leftKey,
                options.rightKey,
                options.jumpKey,
                options.sneakKey);

        List<KeyBinding> rightColumn = present(
                options.inventoryKey,
                options.dropKey,
                options.togglePerspectiveKey,
                options.playerListKey,
                options.chatKey,
                find(options, ZOOMIFY_ZOOM),
                find(options, NT_TOGGLE_FOG),
                find(options, SVC_PUSH_TO_TALK));

        int center = this.width / 2;
        int leftX = center - CENTER_GAP - BUTTON_WIDTH;
        int rightX = center + CENTER_GAP;
        int count = Math.max(leftColumn.size(), rightColumn.size());

        int sliderY = 40;
        this.addDrawableChild(options.getMouseSensitivity()
                .createWidget(options, center - SLIDER_WIDTH / 2, sliderY, SLIDER_WIDTH));

        int doneY = this.height - 27;
        this.gridTop = sliderY + BUTTON_HEIGHT + 12;
        this.gridBottom = doneY - 8;

        int viewport = Math.max(0, this.gridBottom - this.gridTop);
        int contentHeight = count * ROW_SPACING;
        this.maxScroll = Math.max(0, contentHeight - viewport);

        this.contentTop = this.maxScroll > 0
                ? this.gridTop
                : this.gridTop + (viewport - contentHeight) / 2;
        this.scroll = MathHelper.clamp(this.scroll, 0, this.maxScroll);

        for (int i = 0; i < count; i++) {
            if (i < leftColumn.size()) {
                addRow(leftColumn.get(i), leftX, i, false);
            }
            if (i < rightColumn.size()) {
                addRow(rightColumn.get(i), rightX, i, true);
            }
        }

        this.addDrawableChild(ButtonWidget
                .builder(ScreenTexts.DONE, b -> this.close())
                .dimensions(center - 100, doneY, 200, BUTTON_HEIGHT)
                .build());

        this.updatePositions();
        this.updateButtons();
    }

    private void addRow(KeyBinding binding, int x, int index, boolean rightColumn) {
        ButtonWidget button = this.addSelectableChild(ButtonWidget
                .builder(binding.getBoundKeyLocalizedText(), b -> {
                    this.selected = binding;
                    this.updateButtons();
                })
                .dimensions(x, this.contentTop + index * ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        this.rows.add(new Row(binding, button, Text.translatable(binding.getTranslationKey()),
                rightColumn, index));
    }

    private void updatePositions() {
        for (Row row : this.rows) {
            int y = this.contentTop + row.index() * ROW_SPACING - this.scroll;
            row.button().setY(y);
            row.button().visible = y + BUTTON_HEIGHT > this.gridTop && y < this.gridBottom;
        }
    }

    private void setScroll(int value) {
        this.scroll = MathHelper.clamp(value, 0, this.maxScroll);
        this.updatePositions();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.maxScroll > 0) {
            this.setScroll(this.scroll - (int) Math.signum(amount) * ROW_SPACING);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private int scrollbarX() {
        return this.width / 2 + CENTER_GAP + BUTTON_WIDTH + 90;
    }

    private void scrollToMouse(double mouseY) {
        int viewport = this.gridBottom - this.gridTop;
        if (viewport <= 0) return;
        double pct = (mouseY - this.gridTop) / (double) viewport;
        this.setScroll((int) Math.round(pct * this.maxScroll));
    }

    private static List<KeyBinding> present(KeyBinding... bindings) {
        List<KeyBinding> list = new ArrayList<>();
        for (KeyBinding binding : bindings) {
            if (binding != null) list.add(binding);
        }
        return list;
    }

    private static KeyBinding find(GameOptions options, String translationKey) {
        for (KeyBinding binding : options.allKeys) {
            if (binding.getTranslationKey().equals(translationKey)) return binding;
        }
        return null;
    }

    private boolean conflicts(KeyBinding binding) {
        if (binding.isUnbound()) return false;
        for (KeyBinding other : MinecraftClient.getInstance().options.allKeys) {
            if (other != binding && binding.equals(other)) return true;
        }
        return false;
    }

    private void updateButtons() {
        for (Row row : this.rows) {
            MutableText bound = row.binding().getBoundKeyLocalizedText().copy();
            if (row.binding() == this.selected) {
                row.button().setMessage(Text.literal("> ")
                        .append(bound.formatted(Formatting.YELLOW))
                        .append(" <")
                        .formatted(Formatting.YELLOW));
            } else if (this.conflicts(row.binding())) {
                row.button().setMessage(bound.formatted(Formatting.RED));
            } else {
                row.button().setMessage(bound);
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingScrollbar = false;
        if (!this.receivedPress) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.receivedPress = true;

        if (this.selected != null) {
            MinecraftClient.getInstance().options.setKeyCode(this.selected,
                    InputUtil.Type.MOUSE.createFromCode(button));
            this.selected = null;
            KeyBinding.updateKeysByCode();
            this.updateButtons();
            return true;
        }

        if (this.maxScroll > 0) {
            int barX = this.scrollbarX();
            if (mouseX >= barX && mouseX < barX + SCROLLBAR_WIDTH
                    && mouseY >= this.gridTop && mouseY < this.gridBottom) {
                this.draggingScrollbar = true;
                this.scrollToMouse(mouseY);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.draggingScrollbar) {
            this.scrollToMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.selected != null) {
            MinecraftClient.getInstance().options.setKeyCode(this.selected,
                    keyCode == GLFW.GLFW_KEY_ESCAPE
                            ? InputUtil.UNKNOWN_KEY
                            : InputUtil.fromKeyCode(keyCode, scanCode));
            this.selected = null;
            KeyBinding.updateKeysByCode();
            this.updateButtons();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        context.enableScissor(0, this.gridTop, this.width, this.gridBottom);
        for (Row row : this.rows) {
            if (!row.button().visible) {
                continue;
            }
            row.button().render(context, mouseX, mouseY, delta);

            int textY = row.button().getY() + (BUTTON_HEIGHT - this.textRenderer.fontHeight) / 2 + 1;
            if (row.rightColumn()) {
                context.drawTextWithShadow(this.textRenderer, row.label(),
                        row.button().getX() + BUTTON_WIDTH + LABEL_GAP, textY, 0xFFFFFF);
            } else {
                int labelWidth = this.textRenderer.getWidth(row.label());
                context.drawTextWithShadow(this.textRenderer, row.label(),
                        row.button().getX() - LABEL_GAP - labelWidth, textY, 0xFFFFFF);
            }
        }
        context.disableScissor();

        if (this.maxScroll > 0) {
            int barX = this.scrollbarX();
            int viewport = this.gridBottom - this.gridTop;
            int contentHeight = viewport + this.maxScroll;

            context.fill(barX, this.gridTop, barX + SCROLLBAR_WIDTH, this.gridBottom, 0xFF000000);
            int thumbHeight = Math.max(16, viewport * viewport / contentHeight);
            int thumbY = this.gridTop + (viewport - thumbHeight) * this.scroll / this.maxScroll;
            context.fill(barX, thumbY, barX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFFC0C0C0);
        }
    }

    @Override
    public void removed() {
        MinecraftClient.getInstance().options.write();
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }
}