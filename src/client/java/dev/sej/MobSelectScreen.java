package dev.sej;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

/** Mob picker for the spawn egg: animals on the left, monsters on the right. */
public class MobSelectScreen extends Screen {

    private static final int FULL_WIDTH = 204;
    private static final int HALF_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 24;
    private static final int HEADER = 14;

    private final Hand hand;
    private final List<ButtonWidget> mobButtons = new ArrayList<>();
    private final List<ItemStack> icons = new ArrayList<>();
    private int columnsTop;
    /** False until a click starts on this screen, so the release from the click that opened it is ignored. */
    private boolean receivedPress;

    public MobSelectScreen(Hand hand) {
        super(Text.translatable("sej.spawn_egg.title"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        this.mobButtons.clear();
        this.icons.clear();

        EntityType<?> current = null;
        if (this.client != null && this.client.player != null) {
            current = MobSpawnEggItem.getMob(this.client.player.getStackInHand(this.hand));
        }

        int left = this.width / 2 - FULL_WIDTH / 2;
        int right = left + FULL_WIDTH - HALF_WIDTH;
        this.columnsTop = this.height / 6 + 16;
        int buttonsTop = this.columnsTop + HEADER;

        // Animals left, monsters right. A row with only one mob (the wolf) is centred.
        List<EntityType<?>> animals = MobSpawnEggItem.ANIMALS;
        List<EntityType<?>> monsters = MobSpawnEggItem.MONSTERS;
        int rows = Math.max(animals.size(), monsters.size());
        int center = this.width / 2 - HALF_WIDTH / 2;
        for (int row = 0; row < rows; row++) {
            int y = buttonsTop + row * SPACING;
            EntityType<?> animal = row < animals.size() ? animals.get(row) : null;
            EntityType<?> monster = row < monsters.size() ? monsters.get(row) : null;
            if (animal != null && monster != null) {
                this.addMobButton(animal, left, y, current);
                this.addMobButton(monster, right, y, current);
            } else {
                this.addMobButton(animal != null ? animal : monster, center, y, current);
            }
        }

        this.addDrawableChild(ButtonWidget
                .builder(ScreenTexts.CANCEL, b -> this.close())
                .dimensions(left, buttonsTop + rows * SPACING + 8, FULL_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void addMobButton(EntityType<?> type, int x, int y, EntityType<?> current) {
        ButtonWidget button = ButtonWidget
                .builder(type.getName(), b -> this.select(type))
                .dimensions(x, y, HALF_WIDTH, BUTTON_HEIGHT)
                .build();
        button.active = type != current; // the egg's current mob shows greyed out
        this.addDrawableChild(button);

        ItemStack icon = new ItemStack(MobSpawnEggItem.ITEM);
        MobSpawnEggItem.setMob(icon, type);
        this.mobButtons.add(button);
        this.icons.add(icon);
    }

    private void select(EntityType<?> type) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            ItemStack held = client.player.getStackInHand(this.hand);
            if (held.isOf(MobSpawnEggItem.ITEM)) {
                MobSpawnEggItem.setMob(held, type); // instant feedback; the server confirms it
            }
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(this.hand);
        buf.writeIdentifier(Registries.ENTITY_TYPE.getId(type));
        ClientPlayNetworking.send(MobSpawnEggItem.SELECT_MOB, buf);
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        int left = this.width / 2 - FULL_WIDTH / 2;
        int right = left + FULL_WIDTH - HALF_WIDTH;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("sej.spawn_egg.animals"),
                left + HALF_WIDTH / 2, this.columnsTop, 0xA0A0A0);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("sej.spawn_egg.monsters"),
                right + HALF_WIDTH / 2, this.columnsTop, 0xA0A0A0);

        // Each mob's egg, in its colours, at the left end of its button.
        for (int i = 0; i < this.mobButtons.size(); i++) {
            ButtonWidget button = this.mobButtons.get(i);
            context.drawItem(this.icons.get(i), button.getX() + 2, button.getY() + 2);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.receivedPress = true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.receivedPress) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}