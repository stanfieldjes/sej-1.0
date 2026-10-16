package dev.sej;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Beta-style item list shown to the right of the survival inventory in creative mode,
 * toggled by a chest above the right-hand inventory column that opens its lid
 * while the list is shown.
 *
 * Left click an item: full stack on the cursor. Right click: a single item.
 * Shift + left click: full stack straight into the first empty inventory slot.
 * Click the list while holding something: destroys the held stack.
 * Scroll wheel or the arrows: change page.
 */
public class ItemListPanel {

    // Kept static so the list stays open on the same page between inventory opens.
    private static boolean open = false;
    private static int page = 0;
    private static boolean showing = false;

    // Chest button: top right of the inventory, centred over the right-hand column
    // (slot x = 152) and mirroring the helmet slot. The fully open lid rises about
    // 12px, which stays just inside the inventory frame from here.
    private static final int BUTTON_X = 152;
    private static final int BUTTON_Y = 8;
    private static final int BUTTON_SIZE = 16;
    private static final float DEG = (float) Math.PI / 180f;

    // A real chest block entity, so the button is the vanilla 3D model and the lid
    // uses vanilla's own animation (and resource pack textures).
    private static ChestBlockEntity chest;

    private static final int SLOT = 18;
    private static final int SIDE_PAD = 7;
    private static final int HEADER = 15;
    private static final int BOTTOM_PAD = 7;
    private static final int GAP = 2;
    private static final int MAX_COLS = 9;
    private static final int ARROW = 10;

    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_SHADOW = 0xFF555555;
    private static final int COLOR_OUTLINE = 0xFF000000;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_LABEL = 0x404040;
    private static final int COLOR_HIGHLIGHT = 0x80FFFFFF;

    private final PlayerScreenHandler handler;

    private boolean visible;
    private boolean fits;
    private int buttonX, buttonY;
    private int left, top, cols, rows, panelW, panelH;
    private boolean swallowRelease;

    public ItemListPanel(PlayerScreenHandler handler) {
        this.handler = handler;
    }

    /** Used to hide the status effect column while the list sits on top of it. */
    public static boolean isShowing() {
        return showing;
    }

    private static ChestBlockEntity chest() {
        if (chest == null) {
            chest = new ChestBlockEntity(BlockPos.ORIGIN, Blocks.CHEST.getDefaultState());
            chest.onSyncedBlockEvent(1, open ? 1 : 0);
        }
        return chest;
    }

    /** Called every tick while the inventory is open; steps the lid towards open or closed. */
    public static void tick() {
        ChestBlockEntity c = chest();
        ChestBlockEntity.clientTick(MinecraftClient.getInstance().world, BlockPos.ORIGIN, c.getCachedState(), c);
    }

    public void layout(int guiX, int guiY, int bgW, int bgH, int screenW, boolean inventoryShown) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean creative = client.interactionManager != null && client.interactionManager.hasCreativeInventory();
        visible = creative && inventoryShown;

        buttonX = guiX + BUTTON_X;
        buttonY = guiY + BUTTON_Y;

        left = guiX + bgW + GAP;
        int available = screenW - left - GAP;
        cols = Math.min(MAX_COLS, (available - 2 * SIDE_PAD) / SLOT);
        rows = (bgH - HEADER - BOTTOM_PAD) / SLOT;
        panelW = cols * SLOT + 2 * SIDE_PAD;
        panelH = HEADER + rows * SLOT + BOTTOM_PAD;
        top = guiY;
        fits = cols >= 2 && rows >= 1;

        page = Math.floorMod(page, pageCount());
        showing = visible && open && fits;
    }

    private int perPage() {
        return Math.max(1, cols * rows);
    }

    private int pageCount() {
        int size = ItemList.get().size();
        return Math.max(1, (size + perPage() - 1) / perPage());
    }

    // ------------------------------------------------------------------ rendering

    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!visible) return;
        TextRenderer font = MinecraftClient.getInstance().textRenderer;

        drawChest(context, buttonX, buttonY);

        if (showing) renderList(context, font, mouseX, mouseY);

        if (isOver(mouseX, mouseY, buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)) {
            context.drawTooltip(font, Text.literal(open ? "Hide Item List" : "Item List"), mouseX, mouseY);
        }
    }

    /** Same transforms vanilla uses to draw the chest item in a slot, but with a live lid. */
    private static void drawChest(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        ChestBlockEntity c = chest();
        BlockEntityRenderer<ChestBlockEntity> renderer = client.getBlockEntityRenderDispatcher().get(c);
        if (renderer == null) return;

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x + 8, y + 8, 150);
        matrices.multiplyPositionMatrix(new Matrix4f().scaling(1.0f, -1.0f, 1.0f));
        matrices.scale(16.0f, 16.0f, 16.0f);
        // Vanilla's chest item uses rotation [30, 45, 0], which puts the hinge on the left.
        // Yaw -45 mirrors it: latch on the left face, hinge on the right, so the lid
        // swings open left to right.
        matrices.multiply(new Quaternionf().rotationXYZ(30 * DEG, -45 * DEG, 0));
        matrices.scale(0.625f, 0.625f, 0.625f);
        matrices.translate(-0.5f, -0.5f, -0.5f);
        renderer.render(c, client.getTickDelta(), matrices, context.getVertexConsumers(),
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        context.draw();
        matrices.pop();
    }

    private void renderList(DrawContext context, TextRenderer font, int mouseX, int mouseY) {
        List<ItemStack> items = ItemList.get();

        drawPanel(context, left, top, panelW, panelH);

        int prevX = left + SIDE_PAD;
        int nextX = left + panelW - SIDE_PAD - ARROW;
        int arrowY = top + 4;
        drawArrow(context, font, prevX, arrowY, "<", isOver(mouseX, mouseY, prevX, arrowY, ARROW, ARROW));
        drawArrow(context, font, nextX, arrowY, ">", isOver(mouseX, mouseY, nextX, arrowY, ARROW, ARROW));

        String label = (page + 1) + " / " + pageCount();
        context.drawText(font, label, left + (panelW - font.getWidth(label)) / 2, top + 5, COLOR_LABEL, false);

        int start = page * perPage();
        int hoveredIndex = -1;
        int hoverX = 0, hoverY = 0;

        for (int i = 0; i < perPage(); i++) {
            int sx = left + SIDE_PAD + (i % cols) * SLOT;
            int sy = top + HEADER + (i / cols) * SLOT;
            drawSlot(context, sx, sy);

            int index = start + i;
            if (index < items.size()) {
                context.drawItem(items.get(index), sx + 1, sy + 1);
                if (isOver(mouseX, mouseY, sx + 1, sy + 1, 16, 16)) {
                    hoveredIndex = index;
                    hoverX = sx + 1;
                    hoverY = sy + 1;
                }
            }
        }

        if (hoveredIndex >= 0) {
            // Pushed above the item so the highlight tints it, like vanilla slots.
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 300);
            context.fill(hoverX, hoverY, hoverX + 16, hoverY + 16, COLOR_HIGHLIGHT);
            context.getMatrices().pop();
        }

        if (isOver(mouseX, mouseY, left, top + HEADER, panelW, rows * SLOT)) {
            if (!handler.getCursorStack().isEmpty()) {
                context.drawTooltip(font, Text.literal("Click to destroy held item").formatted(Formatting.GRAY), mouseX, mouseY);
            } else if (hoveredIndex >= 0) {
                context.drawItemTooltip(font, items.get(hoveredIndex), mouseX, mouseY);
            }
        }
    }

    private static void drawPanel(DrawContext c, int x, int y, int w, int h) {
        c.fill(x + 1, y, x + w - 1, y + h, COLOR_OUTLINE);
        c.fill(x, y + 1, x + w, y + h - 1, COLOR_OUTLINE);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, COLOR_SHADOW);
        c.fill(x + 1, y + 1, x + w - 3, y + h - 3, COLOR_LIGHT);
        c.fill(x + 3, y + 3, x + w - 3, y + h - 3, COLOR_PANEL);
    }

    private static void drawSlot(DrawContext c, int x, int y) {
        c.fill(x, y, x + 17, y + 1, COLOR_SLOT_DARK);
        c.fill(x, y, x + 1, y + 17, COLOR_SLOT_DARK);
        c.fill(x + 1, y + 17, x + 18, y + 18, COLOR_LIGHT);
        c.fill(x + 17, y + 1, x + 18, y + 18, COLOR_LIGHT);
        c.fill(x + 1, y + 1, x + 17, y + 17, COLOR_SLOT);
    }

    private static void drawArrow(DrawContext c, TextRenderer font, int x, int y, String glyph, boolean hovered) {
        c.fill(x, y, x + ARROW, y + ARROW, COLOR_OUTLINE);
        c.fill(x + 1, y + 1, x + ARROW - 1, y + ARROW - 1, COLOR_SHADOW);
        c.fill(x + 1, y + 1, x + ARROW - 2, y + ARROW - 2, COLOR_LIGHT);
        c.fill(x + 2, y + 2, x + ARROW - 2, y + ARROW - 2, hovered ? 0xFFDADAFF : COLOR_PANEL);
        c.drawText(font, glyph, x + (ARROW - font.getWidth(glyph)) / 2 + 1, y + 1, COLOR_LABEL, false);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ input

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        if (isOver(mouseX, mouseY, buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)) {
            open = !open;
            chest().onSyncedBlockEvent(1, open ? 1 : 0);
            playSound(open ? SoundEvents.BLOCK_CHEST_OPEN : SoundEvents.BLOCK_CHEST_CLOSE, 0.5F);
            swallowRelease = true;
            return true;
        }

        if (!showing || !isOver(mouseX, mouseY, left, top, panelW, panelH)) return false;
        swallowRelease = true;

        int arrowY = top + 4;
        if (isOver(mouseX, mouseY, left + SIDE_PAD, arrowY, ARROW, ARROW)) {
            turnPage(-1);
            return true;
        }
        if (isOver(mouseX, mouseY, left + panelW - SIDE_PAD - ARROW, arrowY, ARROW, ARROW)) {
            turnPage(1);
            return true;
        }

        // Holding something: dropping it back on the list destroys it.
        if (!handler.getCursorStack().isEmpty()) {
            setCursor(ItemStack.EMPTY);
            return true;
        }

        int index = indexAt(mouseX, mouseY);
        if (index < 0) return true;

        ItemStack picked = ItemList.get().get(index).copy();
        if (button == 0 && Screen.hasShiftDown()) {
            picked.setCount(picked.getMaxCount());
            giveToInventory(picked);
        } else {
            picked.setCount(button == 1 ? 1 : picked.getMaxCount());
            setCursor(picked);
        }
        return true;
    }

    /** Swallows the release that follows a click we handled, so vanilla doesn't throw the cursor stack. */
    public boolean mouseReleased() {
        if (!swallowRelease) return false;
        swallowRelease = false;
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!showing || !isOver(mouseX, mouseY, left, top, panelW, panelH) || amount == 0) return false;
        page = Math.floorMod(page + (amount > 0 ? -1 : 1), pageCount());
        return true;
    }

    private void turnPage(int delta) {
        page = Math.floorMod(page + delta, pageCount());
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static void playSound(SoundEvent sound, float volume) {
        MinecraftClient.getInstance().getSoundManager()
                .play(PositionedSoundInstance.master(sound, 1.0F, volume));
    }

    private int indexAt(double mouseX, double mouseY) {
        int gridX = left + SIDE_PAD;
        int gridY = top + HEADER;
        if (!isOver(mouseX, mouseY, gridX, gridY, cols * SLOT, rows * SLOT)) return -1;
        int col = (int) (mouseX - gridX) / SLOT;
        int row = (int) (mouseY - gridY) / SLOT;
        int index = page * perPage() + row * cols + col;
        return index < ItemList.get().size() ? index : -1;
    }

    // ------------------------------------------------------------------ giving items

    private void setCursor(ItemStack stack) {
        if (ClientPlayNetworking.canSend(Sej.SET_CREATIVE_CURSOR)) {
            handler.setCursorStack(stack.copy());
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeItemStack(stack);
            ClientPlayNetworking.send(Sej.SET_CREATIVE_CURSOR, buf);
        } else if (!stack.isEmpty()) {
            // Server without Sandbox: Endless Journey: fall back to the vanilla creative packet.
            giveToInventory(stack);
        }
    }

    /** Hotbar first, then main inventory, same order as picking items up. */
    private void giveToInventory(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null) return;

        int target = firstEmpty(PlayerScreenHandler.HOTBAR_START, PlayerScreenHandler.HOTBAR_END);
        if (target < 0) target = firstEmpty(PlayerScreenHandler.INVENTORY_START, PlayerScreenHandler.INVENTORY_END);
        if (target < 0) return;

        handler.getSlot(target).setStack(stack.copy());
        client.interactionManager.clickCreativeStack(stack.copy(), target);
    }

    private int firstEmpty(int from, int toExclusive) {
        for (int slot = from; slot < toExclusive; slot++) {
            if (handler.getSlot(slot).getStack().isEmpty()) return slot;
        }
        return -1;
    }
}